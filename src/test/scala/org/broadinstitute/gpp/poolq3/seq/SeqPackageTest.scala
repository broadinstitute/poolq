/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.seq

import org.broadinstitute.gpp.poolq3.gen.{acgtn, barcode, dnaSeq, nonEmptyDnaSeq}
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks._

class SeqPackageTest extends AnyFlatSpec {

  "complement" should "complement a string of DNA" in {
    complement("") should be("")
    complement("ACGT") should be("TGCA")
    complement("NAATTTG") should be("NTTAAAC")
  }

  "complement" should "roundtrip arbitrary DNA" in {
    forAll(barcode)(b => complement(complement(b)) should be(b))
  }

  "reverseComplement" should "reverse complement a string of DNA" in {
    reverseComplement("") should be("")
    reverseComplement("ACGT") should be("ACGT")
    reverseComplement("NTTGTATTTA") should be("TAAATACAAN")
  }

  "reverseComplement" should "roundtrip arbitrary DNA" in {
    forAll(barcode)(b => reverseComplement(reverseComplement(b)) should be(b))
  }

  "ndist" should "always be >= 0" in {
    forAll(barcode, barcode)((x, y) => countMismatches(x, y) should be >= 0)
  }

  it should "be 0 iff and only iff x = y" in {
    forAll(barcode, barcode) { (x, y) =>
      val xydist = countMismatches(x, y)
      if (x != y) {
        xydist should be > 0
        countMismatches(x, x) should be(0)
        countMismatches(y, y) should be(0)
      } else xydist should be(0)
    }
  }

  it should "be symmetric" in {
    forAll(barcode, barcode)((x, y) => countMismatches(x, y) should be(countMismatches(y, x)))
  }

  it should "satisfy the triangle inequality in our domain" in {
    forAll(barcode, barcode, barcode) { (x, y, z) =>
      (countMismatches(x, y) + countMismatches(y, z)) should be >= countMismatches(x, z)
    }
  }

  "isDna" should "return true for ACGTN bases" in {
    forAll(nonEmptyDnaSeq(acgtn))(s => isDna(s) should be(true))
  }

  it should "return false for non ACGTN bases" in {
    val nonDnaChar =
      Gen.choose(Char.MinValue, Char.MaxValue).suchThat {
        Set('A', 'C', 'G', 'T', 'N').contains(_) == false
      }
    forAll(nonEmptyDnaSeq(acgtn), nonEmptyDnaSeq(acgtn), nonDnaChar) { (s1, s2, c) =>
      isDna(s1 + c.toString + s2) should be(false)
    }
  }

  "nCount" should "return at most max Ns" in {
    nCount("AAANAN".toCharArray, 0) should be(1)
    nCount("AAANAN".toCharArray, 1) should be(1)
    nCount("AAANAN".toCharArray, 2) should be(2)
    nCount("AAANAN".toCharArray, 3) should be(2)
    nCount("TGNTA".toCharArray, 0) should be(1)
  }

  it should "handle random sequences" in {
    forAll(dnaSeq(acgtn)) { s =>
      val chars = s.toCharArray
      s.indices.foreach { idx =>
        // the min/max thing is to deal with weirdness if max is 0 - we have to find an N to exit early
        nCount(chars, idx) should be(math.min(math.max(idx, 1), nCount(chars)))
      }
    }
  }

}
