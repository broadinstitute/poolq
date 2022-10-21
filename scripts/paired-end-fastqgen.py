#!/usr/bin/env python3
import random
from fastqgen3 import *

def print_setup(cond, ref, fq1, fq2, fq3, matrix, min_stagger, max_stagger, ref_barcode_length, max_trailing_bases):
    read_length = min_stagger + 5 + ref_barcode_length + max_trailing_bases
    
    def exactly_once_in_order(data):
        used = {}
        ids = []
        for (_, x) in data:
            if used.get(x, 0) == 0:
                ids.append(x)
                used[x] = 1
        return ids

    # generate random sample barcodes
    barcodes = random_barcodes(8, 6)
    wells = generate_wells()

    # generate conditions and print it
    conditions = [(b, random.choice(wells)) for b in barcodes]
    conditions.sort()
    write_csv(cond, conditions)

    # generate the reference and print it
    rs = random_barcodes(ref_barcode_length, 32)
    reference1 = rs[:ref_barcode_length]
    reference2 = rs[ref_barcode_length:]
    reference = []
    n = 1
    for r1 in reference1:
        for r2 in reference2:
            reference.append((r1 + ';' + r2, generate_brdn(n)))
            n += 1
    reference.sort()
    write_csv(ref, reference)

    col_ids = exactly_once_in_order(conditions)

    # now generate scores and fastq files
    scores = {}
    with open(fq1, 'w') as fastq1:
        with open(fq2, 'w') as fastq2:
            with open(fq3, 'w') as fastq3:
                for i in range(0, 2000):
                    (col_barcode, col_id) = random.choice(conditions)
                    r1 = random.choice(reference1)
                    r2 = random.choice(reference2)
                    row_barcode = r1 + ';' + r2

                    # increment the scores dict
                    scores[(col_id, row_barcode)] = scores.get((col_id, row_barcode), 0) + 1

                    # generate FASTQ records and print them
                    readid = fastq_read_id(i)

                    # the sample fastq is easy
                    col_qual = random_qual(len(col_barcode))
                    print_fastq_record2(fastq1, (readid, col_barcode, col_qual))

                    # the other two are harder
                    fseq = random_length_seq(min_stagger, max_stagger) + 'CACCG' + r1
                    fseq += random_seq(read_length - (len(fseq)))
                    frow_qual = random_qual(read_length)
                    print_fastq_record2(fastq2, (readid, fseq, frow_qual))

                    rseq = random_length_seq(min_stagger, max_stagger) + 'CGGTG' + r2
                    rseq += random_seq(read_length - (len(rseq)))
                    rrow_qual = random_qual(read_length)
                    print_fastq_record2(fastq3, (readid, rseq, rrow_qual))

    # use the dict to write a scores file
    with open(matrix, 'w') as file:
        # write the header
        header = ['Construct Barcode', 'Construct IDs'] + col_ids
        file.write('\t'.join(header) + '\n')

        for (rbc, rid) in reference:
            row = [rbc, rid]
            sc = [str(scores.get((cid, rbc), 0)) for cid in col_ids]
            file.write('\t'.join(row + sc) + '\n')


def print_paired_end_setup(cond, ref, fq1, fq2, fq3, matrix):
    print_setup(cond, ref, fq1, fq2, fq3, matrix, 12, 15, 20, 16)


# -------------------------------------------------------------------------------
#  Main
# -------------------------------------------------------------------------------

if __name__ == '__main__':
    print_paired_end_setup('conditions.csv',
                           'reference.csv',
                           'paired-end.barcode_1.fastq',
                           'paired-end.f.fastq',
                           'paired-end.r.fastq',
                           'expected-counts.txt')
