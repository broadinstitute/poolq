/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import munit.FunSuite

class KeyMaskTest extends FunSuite:

  test("KeyMask.apply should construct the correct key mask from a pattern") {
    val km0 = KeyMask("NNNNNNNNNNNNNNNNN")
    assertEquals(km0, KeyMask.fromString(17, "1-17"))
    val km1 = KeyMask("NNNNNNNNNNNNNNNNNnNN")
    assertEquals(km1, KeyMask.fromString(20, "1-17,19-20"))
    val km2 = KeyMask("nNNNNNNNNNNNNNNNNNnNNn")
    assertEquals(km2, KeyMask.fromString(22, "2-18,20-21"))
    val km3 = KeyMask("nnnNNNNNNNNNNNNNNNNNnNNnN")
    assertEquals(km3, KeyMask.fromString(25, "4-20,22-23,25"))
    val km4 = KeyMask("nnnnNNNNNNNNNNNNNNNNNNNNnnnnnn")
    assertEquals(km4, KeyMask.fromString(30, "5-24"))
  }

  test("should handle a very large context seq") {
    //                 0                                                                                                   1                                                                                                   2
    //                 0         1         2         3         4         5         6         7         8         9         0         1         2         3         4         5         6         7         8         9         0         1         2         3
    //                 012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
    val km5 = KeyMask(
      "caccgNNNNNNNNNNNNNNNNNNNNnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnNNNNNNNNNNNNNNNNNNNNN"
    )
    assertEquals(km5.contextLength, 240)
    assertEquals(km5.keyLengthInBases, 41)
    assertEquals(km5.keyRanges, Seq(KeyRange(5, 24), KeyRange(219, 239)))
  }

  test("KeyMask.fromString should compute the correct key mask from a list of key ranges in either syntax") {
    val km1 = KeyMask.fromString(23, "4-20,22-23")
    assertEquals(km1, KeyMask(23, Seq(KeyRange(3, 19), KeyRange(21, 22))))
    val km2 = KeyMask.fromString(23, "4..20,22..23")
    assertEquals(km2, KeyMask(23, Seq(KeyRange(3, 19), KeyRange(21, 22))))
  }

  test("KeyMask.mergeAdjacent should handle an empty list") {
    assertEquals(KeyMask.mergeAdjacent(Seq[KeyRange]()), Seq[KeyRange]())
  }

  test("should handle a singleton") {
    assertEquals(KeyMask.mergeAdjacent(Seq(KeyRange(3, 19))), Seq(KeyRange(3, 19)))
  }

  test("should handle disjoint ranges") {
    assertEquals(KeyMask.mergeAdjacent(Seq(KeyRange(3, 19), KeyRange(21, 22))), Seq(KeyRange(3, 19), KeyRange(21, 22)))
  }

  test("should merge contiguous ranges") {
    assertEquals(KeyMask.mergeAdjacent(Seq(KeyRange(3, 21), KeyRange(21, 22))), Seq(KeyRange(3, 22)))
  }

  test("should merge adjacent ranges") {
    assertEquals(
      KeyMask.mergeAdjacent(Seq(KeyRange(1, 9), KeyRange(10, 12), KeyRange(14, 17))),
      Seq(KeyRange(1, 12), KeyRange(14, 17))
    )
    assertEquals(KeyMask.fromString(10, "1,2..4,5,6-8,9"), KeyMask.fromString(10, "1-9"))
  }

  test("KeyMask#indexedRanges should construct ranges from a simple pattern") {
    assertEquals(KeyMask.parsePatternRanges("NNNN"), List(KeyRange(0, 3)))
  }

  test("should handle leading and trailing gaps") {
    assertEquals(KeyMask.parsePatternRanges("nNNNNn"), List(KeyRange(1, 4)))
  }

  test("should handle an interior gap") {
    assertEquals(KeyMask.parsePatternRanges("nNNnNNn"), List(KeyRange(1, 2), KeyRange(4, 5)))
  }

end KeyMaskTest
