#!/usr/bin/env python
import csv
import random
import string

#-------------------------------------------------------------------------------
# Constants
#-------------------------------------------------------------------------------

bases = ['A', 'C', 'G', 'T']

qual = [chr(i) for i in range(35, 75)]


#-------------------------------------------------------------------------------
# Utility functions
#-------------------------------------------------------------------------------

def read_barcodes(filename):
    with open(filename, 'rU') as fh:
        reader = csv.reader(fh, delimiter=',')
        return [row[0] for row in reader]
    

def random_string(alphabet, length):
    return string.join([random.choice(alphabet) for i in range(0, length)], '')


def random_seq(length):
    return random_string(bases, length)


def random_length_seq(min_length, max_length):
    length = random.randint(min_length, max_length)
    return random_seq(length)


def random_qual(length):
    return random_string(qual, length)


def fastq_read_id(number):
    return '@HWUSI-EAS100R:6:23:398:3989#' + str(number)


def gaussian_qual(length, mean, var):
    '''
    returns a quality score sampled from a Gaussian distribution
    with the provided mean and standard deviation
    '''
    def trim(x):
        rounded = round(x)
        if rounded < 35:
            return 35
        elif rounded > 74:
            return 74
        else:
            return int(rounded)

    return [trim(random.gauss(mean, var)) for i in range(0, length)]


def random_barcodes(length, number):
    barcodes = {}
    while len(barcodes.keys()) < number:
        barcode = random_seq(length)
        barcodes[barcode] = 0
    return sorted(barcodes.keys())


def maybe_mutate_seq(seq, quals):
    chars = list(seq)
    for i in range(0, len(seq)):
        if quals[i] < 36 and random.random() < 0.7:
            chars[i] = 'N'
    return string.join(chars, '')


def single_fastq(barcode, seq, stuffer, qual_mean, qual_stddev, number):
    readid = fastq_read_id(number)
    read_seq = barcode + stuffer + seq
    quals = gaussian_qual(len(read_seq), qual_mean, qual_stddev)
    mutated_seq = maybe_mutate_seq(read_seq, quals)
    return (readid, mutated_seq, quals)


def print_fastq_record(file, fastq):
    (readid, seq, qual) = fastq
    file.write(readid + '\n')
    file.write(seq + '\n')
    file.write('+\n')
    file.write(string.join([chr(q) for q in qual], '') + '\n')


def print_scenario1_fastq_file(filename, data):
    stuffer = random_seq(17) + 'CACCG'
    with open(filename, 'w') as file:
        for (i, (barcode, seq)) in enumerate(data):
            fastq = single_fastq(barcode, seq, stuffer, 55, 5, i + 1)
            print_fastq_record(file, fastq)


def print_scenario2_fastq_file(filename, data):
    stuffer = random_seq(10) + 'CACCG'
    with open(filename, 'w') as file:
        for (i, (barcode, seq)) in enumerate(data):
            stagger = random_length_seq(0, 7)
            stagger_len = len(stagger)
            fill_len = 7 - stagger_len
            fill = random_seq(fill_len)
            fastq = single_fastq(barcode, seq + fill, stagger + stuffer, 55, 5, i + 1)
            print_fastq_record(file, fastq)

            
def print_scenario3_fastq_file(filename1, filename2, data):
    # print the construct file
    with open(filename1, 'w') as file:
        stuffer = random_seq(24) + 'CACCG'
        for (i, (_, seq)) in enumerate(data):
            fastq = single_fastq('', seq, stuffer, 55, 5, i + 1)
            print_fastq_record(file, fastq)

    # print the barcode file
    with open(filename2, 'w') as file:
        for(i, (barcode, _)) in enumerate(data):
            fastq = single_fastq(barcode, '', '', 55, 5, i + 1)
            print_fastq_record(file, fastq)


def print_scenario4_fastq_file(filename1, filename2, data):
    # print the construct file
    with open(filename1, 'w') as file:
        stuffer = random_seq(18) + 'CACCG'
        for (i, (_, seq)) in enumerate(data):
            stagger = random_length_seq(0, 7)
            stagger_len = len(stagger)
            fill_len = 7 - stagger_len
            fill = random_seq(fill_len)
            fastq = single_fastq('', seq + fill, stagger + stuffer, 55, 5, i + 1)
            print_fastq_record(file, fastq)

    # print the barcode file
    with open(filename2, 'w') as file:
        for(i, (barcode, _)) in enumerate(data):
            fastq = single_fastq(barcode, '', '', 55, 5, i + 1)
            print_fastq_record(file, fastq)


        
#-------------------------------------------------------------------------------
# Main
#-------------------------------------------------------------------------------
        

if __name__ == '__main__':
    barcodes = read_barcodes('Conditions.csv')
    seqs = read_barcodes('Reference.csv')

    data = [(random.choice(barcodes), random.choice(seqs)) for _ in range(0, 1000)]
    
    print_scenario1_fastq_file('scenario1/scenario1.fastq', data)
    print_scenario2_fastq_file('scenario2/scenario2.fastq', data)
    print_scenario3_fastq_file('scenario3/scenario3.1.fastq', 'scenario3/scenario3.barcode_1.fastq', data)
    print_scenario4_fastq_file('scenario4/scenario4.1.fastq', 'scenario4/scenario4.barcode_1.fastq', data)

