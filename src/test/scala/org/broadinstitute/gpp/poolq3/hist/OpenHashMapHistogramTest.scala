/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.hist

import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.Prop.forAll

class OpenHashMapHistogramTest extends FunSuite with ScalaCheckSuite:

  test("OpenHashMapHistogram should track frequencies") {
    val h = new OpenHashMapHistogram[String]

    val _ = h.increment("AAAA")
    val _ = h.increment("AAAA")
    val _ = h.increment("AAAA")
    val _ = h.increment("AAAA")

    assertEquals(h.count("AAAA"), 4L)
    assertEquals(h.count("CCCC"), 0L)
  }

  property("OpenHashMapHistogram should track frequencies for arbitrary data") {
    forAll { (data: List[Int]) =>
      val expectedCounts: Map[Int, Long] = data.groupBy(identity).view.mapValues(_.length.toLong).toMap

      val hist = new OpenHashMapHistogram[Int]
      data.foreach(hist.increment)

      expectedCounts.foreach { case (x, count) => assertEquals(hist.count(x), count) }
    }
  }

end OpenHashMapHistogramTest
