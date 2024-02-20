/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.gen

import org.broadinstitute.gpp.poolq3.seq.Bases
import org.scalacheck.Gen

type Base = Char

val acgt: Gen[Base] = Gen.oneOf(Bases)

val acgtn: Gen[Base] = Gen.oneOf(Bases :+ 'N')

def nonEmptyDnaSeq(bases: Gen[Base]): Gen[String] =
  Gen.nonEmptyListOf(bases).flatMap(_.mkString)

def dnaSeq(bases: Gen[Base]): Gen[String] = Gen.listOf(bases).flatMap(_.mkString)

def dnaSeqOfN(bases: Gen[Base], n: Int): Gen[String] =
  Gen.listOfN(n, bases).flatMap(_.mkString)

def dnaSeqMaxN(bases: Gen[Base], n: Int): Gen[String] = Gen.sized(size => dnaSeqOfN(bases, math.abs(size) % n))

val barcode: Gen[String] = dnaSeqOfN(acgt, 20)

val barcodeN: Gen[String] = dnaSeqOfN(acgtn, 20)
