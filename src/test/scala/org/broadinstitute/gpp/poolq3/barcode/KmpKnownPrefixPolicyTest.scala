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

class KmpKnownPrefixPolicyTest extends FunSuite with ScalaCheckSuite:

  val fixed = "NNNNNNNNNNNN"

  test("find should find a barcode") {
    forAll(dnaSeqMaxN(acgtn, 7), dnaSeqOfN(acgt, 5), dnaSeqOfN(acgtn, 20)) {
      (variable: String, prefix: String, barcode: String) =>
        val read = Read("id", variable + fixed + prefix + barcode)
        val policy = KmpKnownPrefixPolicy(prefix, barcode.length, Some(7))
        val found: Option[FoundBarcode] = policy.find(read)
        assertEquals(found, Some(FoundBarcode(barcode.toCharArray, variable.length + fixed.length + prefix.length)))
    }
  }

  test("should work for these cases") {
    val reads = Seq(
      "GGTCACCGATCAACGCACCTCCATCCACCGACCACACAGCTTGGACCTTT",
      "GGTCACCGTCACCTCCATCCACCGACCACACAGCTTGGACCTTTGGCATG",
      "TTGACAATCGATGTACACCTCCATCCACCGACCACACAGCTTGGACCTTT"
    )
    val policy = KmpKnownPrefixPolicy("CACCG", 20, Some(18))
    reads.foreach { read =>
      val r = new Read("id", read)
      val actual = policy.find(r)
      assert(actual.isDefined)
    }
  }

  test("should not find a barcode that's before the search window") {
    val prefix = "CACCG"
    val barcodeLength = 20
    val minPrefixPos = 22
    val policy = KmpKnownPrefixPolicy(prefix, barcodeLength, Some(minPrefixPos))
    val nonPrefixBase = Gen.oneOf('A', 'T', 'N')
    forAll(
      dnaSeqMaxN(nonPrefixBase, minPrefixPos - prefix.length),
      Gen.chooseNum(0, minPrefixPos),
      dnaSeqOfN(nonPrefixBase, barcodeLength)
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
    val policy = KmpKnownPrefixPolicy(prefix, barcodeLength, Some(minPrefixPos), Some(maxPrefixPos))
    val nonPrefixBase = Gen.oneOf('A', 'T', 'N')
    forAll(dnaSeqOfN(nonPrefixBase, maxPrefixPos + 1), dnaSeqOfN(nonPrefixBase, barcodeLength)) { (pre, barcode) =>
      val seq = pre + prefix + barcode
      assertEquals(policy.find(Read("id", seq)), None)
    }
  }

end KmpKnownPrefixPolicyTest
