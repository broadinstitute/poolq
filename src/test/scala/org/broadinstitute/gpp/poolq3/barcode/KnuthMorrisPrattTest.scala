/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import munit.{FunSuite, ScalaCheckSuite}
import org.broadinstitute.gpp.poolq3.gen.{acgtn, dnaSeq}
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class KnuthMorrisPrattTest extends FunSuite with ScalaCheckSuite:

  private val prefixGen: Gen[String] = dnaSeq(acgtn).suchThat(!_.contains("CACCG"))

  test("KnuthMorrisPratt should find CACCG") {
    val kmp = new KnuthMorrisPratt("CACCG")
    forAll(prefixGen, dnaSeq(acgtn)) { (s1: String, s2: String) =>
      val s = s"${s1}CACCG$s2"
      assertEquals(kmp.search(s), Some(s1.length))
    }
  }

end KnuthMorrisPrattTest
