/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.hist

import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.Prop.forAll

class OpenHashMapHistogramTest extends FunSuite with ScalaCheckSuite {

  test("OpenHashMapHistogram should track frequencies") {
    val h = new OpenHashMapHistogram[String]

    h.increment("AAAA")
    h.increment("AAAA")
    h.increment("AAAA")
    h.increment("AAAA")

    assertEquals(h.count("AAAA"), 4)
    assertEquals(h.count("CCCC"), 0)
  }

  property("OpenHashMapHistogram should track frequencies for arbitrary data") {
    forAll { (data: List[Int]) =>
      val expectedCounts: Map[Int, Int] = data.groupBy(identity).view.mapValues(_.length).toMap

      val hist = new OpenHashMapHistogram[Int]
      data.foreach(hist.increment)

      expectedCounts.foreach { case (x, count) => assertEquals(hist.count(x), count) }
    }
  }

}
