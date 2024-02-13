# PoolQ 3.0

Copyright (c) 2024 Genetic Perturbation Platform, The Broad Institute of Harvard and MIT.

[![Build Status](https://github.com/broadinstitute/poolq/actions/workflows/ci.yml/badge.svg)](https://github.com/broadinstitute/poolq/actions/workflows/ci.yml)

## Overview

PoolQ is a counter for indexed samples from next-generation sequencing of pooled DNA. Given a set
of sequencing data files (FASTQ, SAM, or BAM), and a pair of reference files mapping DNA barcodes
to construct or experimental identifiers, PoolQ reads the sequencing data and tallies the
co-occurrence of each pair of barcodes from the two files, yielding a two-dimensional histogram.
The barcodes in one reference file are treated as rows in the histogram; the other correspond to
columns.

PoolQ is capable of locating barcodes within reads using a variety of techniques:

- Fixed location
- Known DNA prefix
- Template matching

It matches barcodes to reference data either exactly or allowing up to one base of mismatch. Currently,
PoolQ does not support matching with gaps or deletions.

In addition to producing a histogram, PoolQ generates a number of reports, which contain statistics and
other information that can be used to troubleshoot experiments. These include match percentages, barcode
locations, matching correlations between barcodes, and lists of frequently-occurring unknown barcodes.

## Documentation
For information on how to run PoolQ and its various modes and options, please see the
[manual](docs/MANUAL.md). We also maintain a [changelog](CHANGELOG.md) listing updates made to PoolQ.

As of version 3.5.0, the source code to PoolQ is available under a [BSD 3-clause license](LICENSE). We
welcome contributions to PoolQ and have created a [contributor guide](CONTRIBUTING.md). Additionally,
we maintain a [list](NOTICE.txt) of other open-source libraries PoolQ depends on, along with links to
associated licenses.

## Changes in PoolQ 3

PoolQ was completely rewritten for version 3. The new code is faster and the codebase is much cleaner
and more maintainable. We have taken the opportunity to make other changes to PoolQ as well.

* There are substantial changes to the command-line interface for the program.
* The default counts file format has changed slightly, although there is a command-line
argument that indicates that PoolQ 3 should write a backwards-compatible counts file. The differences
are in headers only; file parsers should be able to adapt easily.
* The quality file has changed somewhat. Importantly, the definition of certain statistics has changed
slightly, so quality metrics cannot be directly compared between the the new and old versions. In addition,
we no longer provide normalized match counts.

See the [manual](docs/MANUAL.md) for complete details on the differences versions 2 and 3.

## PoolQ 2 support

We will continue to make the PoolQ 2.4 artifacts available for download on the
[GPP portal](https://portals.broadinstitute.org/gpp/public/software/poolq). We have no plans to add
features to the code. We will address bugs on a case-by-case basis; in general only critical
bugfixes will be ported to versions prior to 2.4, effective immediately.

## Maintainers

PoolQ was originally developed by John Sullivan and Shuba Gopal of the Broad Institute RNAi Platform. It
is maintained by Mark Tomko of the Broad Institute Genetic Perturbation Platform.

## Contact Us

Your feedback of any kind is much appreciated. Please email us at gpp-informatics@broadinstitute.org.
