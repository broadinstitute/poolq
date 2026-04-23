/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import munit.{FunSuite, ScalaCheckSuite}
import org.broadinstitute.gpp.poolq3.gen.{acgt, acgtn, dnaSeqMaxN, dnaSeqOfN}
import org.broadinstitute.gpp.poolq3.types.Read
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class IndexOfKnownPrefixPolicyTest extends FunSuite with ScalaCheckSuite:

  val fixed = "NNNNNNNNNNNN"

  test("find should find a barcode") {
    forAll(dnaSeqMaxN(acgtn, 7), dnaSeqOfN(acgt, 5), dnaSeqOfN(acgtn, 20)) {
      (variable: String, prefix: String, barcode: String) =>
        val read = Read("id", variable + fixed + prefix + barcode)
        val policy = IndexOfKnownPrefixPolicy(prefix, barcode.length, Some(7))
        val found: Option[FoundBarcode] = policy.find(read)
        assertEquals(found, Some(FoundBarcode(barcode.toCharArray, variable.length + fixed.length + prefix.length)))
    }
  }

  test("should not find a barcode that's before the search window") {
    val prefix = "CACCG"
    val barcodeLength = 20
    val minPrefixPos = 22
    val policy = IndexOfKnownPrefixPolicy(prefix, barcodeLength, Some(minPrefixPos))
    // this generator cannot generate a CACCG prefix because it cannot generate a C
    val notAllBases = Gen.oneOf('A', 'T', 'G')
    forAll(
      dnaSeqMaxN(notAllBases, minPrefixPos - prefix.length),
      Gen.chooseNum(0, minPrefixPos),
      dnaSeqOfN(notAllBases, barcodeLength)
    ) { (bases, prefixPos, barcode) =>
      val pre = bases.take(prefixPos)
      val post = bases.drop(prefixPos)
      val seq = pre + prefix + post + barcode
      assertEquals(policy.find(Read("id", seq)), None)
    }
  }

  test("should not find a barcode that's after the search window") {
    val prefix = "CACCG"
    val barcodeLength = 20
    val minPrefixPos = 22
    val maxPrefixPos = 29
    val policy = IndexOfKnownPrefixPolicy(prefix, barcodeLength, Some(minPrefixPos), Some(maxPrefixPos))
    // this generator cannot generate a CACCG prefix because it cannot generate a C
    val notAllBases = Gen.oneOf('A', 'T', 'G')
    forAll(dnaSeqOfN(notAllBases, maxPrefixPos + 1), dnaSeqOfN(notAllBases, barcodeLength)) { (pre, barcode) =>
      val seq = pre + prefix + barcode
      assertEquals(policy.find(Read("id", seq)), None)
    }
  }

end IndexOfKnownPrefixPolicyTest
