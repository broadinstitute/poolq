/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*

class KeyMaskTest extends AnyFlatSpec:

  "KeyMask.apply" should "construct the correct key mask from a pattern" in {
    val km0 = KeyMask("NNNNNNNNNNNNNNNNN")
    val _ = km0 should be(KeyMask.fromString(17, "1-17"))
    val km1 = KeyMask("NNNNNNNNNNNNNNNNNnNN")
    val _ = km1 should be(KeyMask.fromString(20, "1-17,19-20"))
    val km2 = KeyMask("nNNNNNNNNNNNNNNNNNnNNn")
    val _ = km2 should be(KeyMask.fromString(22, "2-18,20-21"))
    val km3 = KeyMask("nnnNNNNNNNNNNNNNNNNNnNNnN")
    val _ = km3 should be(KeyMask.fromString(25, "4-20,22-23,25"))
    val km4 = KeyMask("nnnnNNNNNNNNNNNNNNNNNNNNnnnnnn")
    km4 should be(KeyMask.fromString(30, "5-24"))
  }

  it should "handle a very large context seq" in {
    //                 0                                                                                                   1                                                                                                   2
    //                 0         1         2         3         4         5         6         7         8         9         0         1         2         3         4         5         6         7         8         9         0         1         2         3
    //                 012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
    val km5 = KeyMask(
      "caccgNNNNNNNNNNNNNNNNNNNNnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnNNNNNNNNNNNNNNNNNNNNN"
    )
    val _ = km5.contextLength should be(240)
    val _ = km5.keyLengthInBases should be(41)
    km5.keyRanges should be(Seq(KeyRange(5, 24), KeyRange(219, 239)))
  }

  "KeyMask.fromString" should "compute the correct key mask from a list of key ranges in either syntax" in {
    val km1 = KeyMask.fromString(23, "4-20,22-23")
    val _ = km1 should be(KeyMask(23, Seq(KeyRange(3, 19), KeyRange(21, 22))))
    val km2 = KeyMask.fromString(23, "4..20,22..23")
    km2 should be(KeyMask(23, Seq(KeyRange(3, 19), KeyRange(21, 22))))
  }

  "KeyMask.mergeAdjacent" should "handle an empty list" in {
    KeyMask.mergeAdjacent(Seq[KeyRange]()) should be(Seq[KeyRange]())
  }

  it should "handle a singleton" in {
    KeyMask.mergeAdjacent(Seq(KeyRange(3, 19))) should be(Seq(KeyRange(3, 19)))
  }

  it should "handle disjoint ranges" in {
    KeyMask.mergeAdjacent(Seq(KeyRange(3, 19), KeyRange(21, 22))) should be(Seq(KeyRange(3, 19), KeyRange(21, 22)))
  }

  it should "merge contiguous ranges" in {
    KeyMask.mergeAdjacent(Seq(KeyRange(3, 21), KeyRange(21, 22))) should be(Seq(KeyRange(3, 22)))
  }

  it should "merge adjacent ranges" in {
    val _ = KeyMask.mergeAdjacent(Seq(KeyRange(1, 9), KeyRange(10, 12), KeyRange(14, 17))) should be(
      Seq(KeyRange(1, 12), KeyRange(14, 17))
    )
    KeyMask.fromString(10, "1,2..4,5,6-8,9") should be(KeyMask.fromString(10, "1-9"))
  }

  "KeyMask#indexedRanges" should "construct ranges from a simple pattern" in {
    KeyMask.parsePatternRanges("NNNN") should be(List(KeyRange(0, 3)))
  }

  it should "handle leading and trailing gaps" in {
    KeyMask.parsePatternRanges("nNNNNn") should be(List(KeyRange(1, 4)))
  }

  it should "handle an interior gap" in {
    KeyMask.parsePatternRanges("nNNnNNn") should be(List(KeyRange(1, 2), KeyRange(4, 5)))
  }

end KeyMaskTest
