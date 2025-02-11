/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.broadinstitute.gpp.poolq3.gen.{acgtn, dnaSeq}
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks.*

class KnuthMorrisPrattTest extends AnyFlatSpec:

  private val prefixGen: Gen[String] = dnaSeq(acgtn).suchThat(!_.contains("CACCG"))

  "KnuthMorrisPratt" should "find CACCG" in {
    val kmp = new KnuthMorrisPratt("CACCG")
    forAll(prefixGen, dnaSeq(acgtn)) { (s1: String, s2: String) =>
      val s = s"${s1}CACCG$s2"
      kmp.search(s) should be(Some(s1.length))
    }
  }

end KnuthMorrisPrattTest
