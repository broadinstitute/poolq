/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.broadinstitute.gpp.poolq3.gen.{acgt, acgtn, dnaSeqMaxN, dnaSeqOfN}
import org.broadinstitute.gpp.poolq3.tools.nanoTimed
import org.broadinstitute.gpp.poolq3.types.Read
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks._

class TemplatePolicyTest extends AnyFlatSpec {

  "KeyMaskPolicy" should "do a thing" in {
    val km = KeyMask("caccgNNNNNnnnnnnnnnttacaNNNNN")
    val kmp = GeneralTemplatePolicy(km, Some(11))

    //                    caccgNNNNNnnnnnnnnnttacaNNNNN
    //                           caccgNNNNNnnnnnnnnnttacaNNNNN
    //                              caccgNNNNNnnnnnnnnnttacaNNNNN
    //         0         1         2         3         4
    //         01234567890123456789012345678901234567890123456789
    val seq = "CTTGTGGAAATGACGAACCACCGATAGCCAAGAATTATTACAAGTTTTAG"

    kmp.find(Read("", seq)) should be(Some(FoundBarcode("ATAGCAGTTT".toCharArray, 23)))
  }

  it should "find a seq at the right edge of the read" in {
    val km = KeyMask("caccgNNNNNnnnnnnnnnttacaNNNNN")
    val kmp = GeneralTemplatePolicy(km, Some(11))

    //         0         1         2         3         4
    //         01234567890123456789012345678901234567890123456789
    val seq = "CTTGTGGAAATGACGAACCACCACCGGCCAAGAATTATTATTACATTTAG"
    //                              caccgNNNNNnnnnnnnnnttacaNNNNN

    kmp.find(Read("", seq)) should be(Some(FoundBarcode("GCCAATTTAG".toCharArray, 26)))
  }

  // this is the same case as above, but I deleted the last base of the seq
  it should "not find a seq past the right edge of the read" in {
    val km = KeyMask("caccgNNNNNnnnnnnnnnttacaNNNNN")
    val kmp = GeneralTemplatePolicy(km, Some(11))

    //         0         1         2         3         4
    //         0123456789012345678901234567890123456789012345678
    val seq = "CTTGTGGAAATGACGAACCACCACCGGCCAAGAATTATTATTACATTTA"
    //                              caccgNNNNNnnnnnnnnnttacaNNNNN

    kmp.find(Read("", seq)) should be(None)
  }

  it should "find a seq at the min start pos" in {
    val km = KeyMask("caccgNNNNNnnnnnnnnnttacaNNNNN")
    val kmp = GeneralTemplatePolicy(km, Some(11))

    //         0         1         2         3         4
    //         01234567890123456789012345678901234567890123456789
    val seq = "CTTGTGGAAATCACCGACCACCACCGGCCATTACATATTATTACATTTAG"
    //                    caccgNNNNNnnnnnnnnnttacaNNNNN

    kmp.find(Read("", seq)) should be(Some(FoundBarcode("ACCACTATTA".toCharArray, 16)))
  }

  // this is the same case as above, but I deleted the first base of the seq
  it should "not find a seq before the min start pos" in {
    val km = KeyMask("caccgNNNNNnnnnnnnnnttacaNNNNN")
    val kmp = GeneralTemplatePolicy(km, Some(11))

    //         0         1         2         3         4
    //         0123456789012345678901234567890123456789012345678
    val seq = "TTGTGGAAATCACCGACCACCACTGGCCATTACATATTATTACATTTAG"
    //                   caccgNNNNNnnnnnnnnnttacaNNNNN

    kmp.find(Read("", seq)) should be(None)
  }

  it should "find a seq at the max start pos" in {
    val km = KeyMask("caccgNNNNNnnnnnnnnnttacaNNNNN")
    val kmp = GeneralTemplatePolicy(km, Some(11), Some(11))

    //         0         1         2         3         4
    //         01234567890123456789012345678901234567890123456789
    val seq = "CTTGTGGAAATCACCGACCACCACCGGCCATTACATATTATTACATTTAG"
    //                    caccgNNNNNnnnnnnnnnttacaNNNNN

    kmp.find(Read("", seq)) should be(Some(FoundBarcode("ACCACTATTA".toCharArray, 16)))
  }

  it should "not find a seq past the max start pos" in {
    val km = KeyMask("caccgNNNNNnnnnnnnnnttacaNNNNN")
    val kmp = GeneralTemplatePolicy(km, Some(5), Some(10))

    //         0         1         2         3         4
    //         01234567890123456789012345678901234567890123456789
    val seq = "CTTGTGGAAATCACCGACCACCACCGGCCATTACATATTATTACATTTAG"
    //                    caccgNNNNNnnnnnnnnnttacaNNNNN

    kmp.find(Read("", seq)) should be(None)
  }

  it should "handle a very long read" in {
    // format: off
    val pattern = "caccgNNNNNNNNNNNNNNNNNNNNnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnNNNNNNNNNNNNNNNNNNNNN"
    val read1   = "CACCGTTTTTTTTTTTTTTTTTTTTAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAATTTTTTTTTTTTTTTTTTTTT"
    val read2   = "CACCGNTTTTTTTTTTTTTTTTTTTAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAATTTTTTTTTTTTTTTTTTTTT"
    val read3   = "GTGGCTTTTTTTTTTTTTTTTTTTTAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAATTTTTTTTTTTTTTTTTTTTT"
    // format: on

    val keymask = KeyMask(pattern)
    val kmp = new GeneralTemplatePolicy(keymask, Some(0))

    kmp.find(Read("", read1)) should be(Some(FoundBarcode("TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT".toCharArray, 5)))
    kmp.find(Read("", read2)) should be(Some(FoundBarcode("NTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT".toCharArray, 5)))
    kmp.find(Read("", read3)) should be(None)
  }

  ignore should "not be too slow given some random barcodes" in {
    val fixed = "ACTTGCATTGCAATGTGA"
    val prefix1 = "CACCG"
    val prefix2 = "TTACA"

    val km = KeyMask("caccgNNNNNnnnnnnnnnttacaNNNNN")
    val kmp = GeneralTemplatePolicy(km, Some(fixed.length))
    val kpp = IndexOfKnownPrefixPolicy("CACCG", 26, Some(fixed.length))

    forAll(dnaSeqMaxN(acgtn, 7), dnaSeqOfN(acgt, 5), dnaSeqOfN(acgtn, 10), dnaSeqOfN(acgt, 5), dnaSeqOfN(acgtn, 5)) {
      (variable: String, r1: String, ns: String, r2: String, rest: String) =>
        val read = Read("id", variable + fixed + prefix1 + r1 + ns + prefix2 + r2 + rest)
        // warm up phase
        val _ = nanoTimed(100)(_ => kmp.find(read))
        val _ = nanoTimed(100)(_ => kpp.find(read))

        // go!
        val (ret1, t1) = nanoTimed(10000)(_ => kmp.find(read))
        Thread.sleep(1000)
        val (ret2, t2) = nanoTimed(10000)(_ => kpp.find(read))

        val ratio1 = t1.toFloat / t2
        println(s"indexof: $t2 ratio: 1.0")
        println(s"keymask: $t1 ratio: $ratio1")

        ret1 should be(ret2)
    }
  }

}
