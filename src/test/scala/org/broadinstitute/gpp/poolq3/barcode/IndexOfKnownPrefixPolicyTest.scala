/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.broadinstitute.gpp.poolq3.gen.{acgt, acgtn, dnaSeqMaxN, dnaSeqOfN}
import org.broadinstitute.gpp.poolq3.types.Read
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks.*

class IndexOfKnownPrefixPolicyTest extends AnyFlatSpec {

  val fixed = "NNNNNNNNNNNN"

  "find" should "find a barcode" in {
    forAll(dnaSeqMaxN(acgtn, 7), dnaSeqOfN(acgt, 5), dnaSeqOfN(acgtn, 20)) {
      (variable: String, prefix: String, barcode: String) =>
        val read = Read("id", variable + fixed + prefix + barcode)
        val policy = IndexOfKnownPrefixPolicy(prefix, barcode.length, Some(7))
        val found: Option[FoundBarcode] = policy.find(read)
        found should be(Some(FoundBarcode(barcode.toCharArray, variable.length + fixed.length + prefix.length)))
    }
  }

  it should "not find a barcode that's before the search window" in {
    val prefix = "CACCG"
    val barcodeLength = 20
    val minPrefixPos = 22
    val policy = IndexOfKnownPrefixPolicy(prefix, barcodeLength, Some(minPrefixPos))
    forAll(
      dnaSeqMaxN(acgtn, minPrefixPos - prefix.length),
      Gen.chooseNum(0, minPrefixPos),
      dnaSeqOfN(acgtn, barcodeLength)
    ) { (bases, prefixPos, barcode) =>
      val pre = bases.take(prefixPos)
      val post = bases.drop(prefixPos)
      val seq = pre + "CACCG" + post + barcode
      policy.find(Read("id", seq)) should be(None)
    }
  }

  it should "not find a barcode that's after the search window" in {
    val prefix = "CACCG"
    val barcodeLength = 20
    val minPrefixPos = 22
    val maxPrefixPos = 29
    val policy = IndexOfKnownPrefixPolicy(prefix, barcodeLength, Some(minPrefixPos), Some(maxPrefixPos))
    forAll(dnaSeqMaxN(acgtn, maxPrefixPos + 1), dnaSeqOfN(acgtn, barcodeLength)) { (pre, barcode) =>
      val seq = pre + "CACCG" + barcode
      policy.find(Read("id", seq)) should be(None)
    }
  }

}
