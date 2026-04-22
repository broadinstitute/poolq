/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.seq

import munit.{FunSuite, ScalaCheckSuite}
import org.broadinstitute.gpp.poolq3.gen.{acgtn, barcode, dnaSeq, nonEmptyDnaSeq}
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class SeqPackageTest extends FunSuite with ScalaCheckSuite:

  test("complement should complement a string of DNA") {
    assertEquals(complement(""), "")
    assertEquals(complement("ACGT"), "TGCA")
    assertEquals(complement("NAATTTG"), "NTTAAAC")
  }

  test("complement should roundtrip arbitrary DNA") {
    forAll(barcode)(b => assertEquals(complement(complement(b)), b))
  }

  test("reverseComplement should reverse complement a string of DNA") {
    assertEquals(reverseComplement(""), "")
    assertEquals(reverseComplement("ACGT"), "ACGT")
    assertEquals(reverseComplement("NTTGTATTTA"), "TAAATACAAN")
  }

  test("reverseComplement should roundtrip arbitrary DNA") {
    forAll(barcode)(b => assertEquals(reverseComplement(reverseComplement(b)), b))
  }

  test("ndist should always be >= 0") {
    forAll(barcode, barcode)((x, y) => assert(countMismatches(x, y) >= 0))
  }

  test("should be 0 iff x = y") {
    forAll(barcode, barcode) { (x, y) =>
      val xydist = countMismatches(x, y)
      if x != y then
        assert(xydist > 0)
        assertEquals(countMismatches(x, x), 0)
        assertEquals(countMismatches(y, y), 0)
      else assertEquals(xydist, 0)
    }
  }

  test("should be symmetric") {
    forAll(barcode, barcode)((x, y) => assertEquals(countMismatches(x, y), countMismatches(y, x)))
  }

  test("should satisfy the triangle inequality in our domain") {
    forAll(barcode, barcode, barcode) { (x, y, z) =>
      assert((countMismatches(x, y) + countMismatches(y, z)) >= countMismatches(x, z))
    }
  }

  test("isDna should return true for ACGTN bases") {
    forAll(nonEmptyDnaSeq(acgtn))(s => assert(isDna(s)))
  }

  test("should return false for non ACGTN bases") {
    val nonDnaChar =
      Gen.choose(Char.MinValue, Char.MaxValue).suchThat {
        Set('A', 'C', 'G', 'T', 'N').contains(_) == false
      }
    forAll(nonEmptyDnaSeq(acgtn), nonEmptyDnaSeq(acgtn), nonDnaChar) { (s1, s2, c) =>
      assert(!isDna(s1 + c.toString + s2))
    }
  }

  test("nCount should return at most max Ns") {
    assertEquals(nCount("AAANAN".toCharArray, 0), 1)
    assertEquals(nCount("AAANAN".toCharArray, 1), 1)
    assertEquals(nCount("AAANAN".toCharArray, 2), 2)
    assertEquals(nCount("AAANAN".toCharArray, 3), 2)
    assertEquals(nCount("TGNTA".toCharArray, 0), 1)
  }

  test("should handle random sequences") {
    forAll(dnaSeq(acgtn)) { s =>
      val chars = s.toCharArray
      s.indices.foreach { idx =>
        // the min/max thing is to deal with weirdness if max is 0 - we have to find an N to exit early
        assertEquals(nCount(chars, idx), math.min(math.max(idx, 1), nCount(chars)))
      }
    }
  }

end SeqPackageTest
