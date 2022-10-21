import csv
import random
import string

# -------------------------------------------------------------------------------
#  Constants
# -------------------------------------------------------------------------------

BASES = ['A', 'C', 'G', 'T']

QUAL = [chr(i) for i in range(35, 75)]

# -------------------------------------------------------------------------------
#  Utility functions
# -------------------------------------------------------------------------------
def generate_wells():
    wells = []
    for r in [chr(i) for i in range(ord('A'), ord('I'))]:
        for i in range(1, 13):
            wells.append(r + '{:02d}'.format(i))
        return wells


def generate_brdn(n):
    return 'BRDN' + '{:010d}'.format(n)


def read_barcodes(filename):
    with open(filename, 'rU') as fh:
        reader = csv.reader(fh, delimiter=',')
        return [row[0] for row in reader]
    

def random_string(alphabet, length):
    return ''.join([random.choice(alphabet) for i in range(0, length)])


def random_seq(length):
    return random_string(BASES, length)


def random_length_seq(min_length, max_length):
    length = random.randint(min_length, max_length)
    return random_seq(length)


def random_qual(length):
    return random_string(QUAL, length)


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


def write_csv(filename, data):
    with open(filename, 'w') as file:
        for (barcode, payload) in data:
            file.write(barcode + "," + payload + '\n')


def single_fastq(barcode, seq, stuffer, qual_mean, qual_stddev, number):
    readid = fastq_read_id(number)
    read_seq = barcode + stuffer + seq
    quals = gaussian_qual(len(read_seq), qual_mean, qual_stddev)
    mutated_seq = maybe_mutate_seq(read_seq, quals)
    return (readid, mutated_seq, quals)


def print_fastq_record(file, fastq):
    (readid, seq, quality) = fastq
    file.write(readid + '\n')
    file.write(seq + '\n')
    file.write('+\n')
    file.write(string.join([chr(q) for q in quality], '') + '\n')


def print_fastq_record2(file, fastq):
    (readid, seq, quality) = fastq
    file.write(readid + '\n')
    file.write(seq + '\n')
    file.write('+\n')
    file.write(quality + '\n')
