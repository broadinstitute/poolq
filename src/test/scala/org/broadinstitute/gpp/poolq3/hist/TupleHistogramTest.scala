/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.hist

import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.Prop.forAll

class TupleHistogramTest extends FunSuite with ScalaCheckSuite {

  test("OpenHashMapHistogram should track frequencies") {
    val h = new TupleHistogram[String]

    h.increment(("AAAA", "TTTT"))
    h.increment(("AAAA", "TTTT"))
    h.increment(("AAAA", "TTTT"))
    h.increment(("AAAA", "TTTT"))

    assertEquals(h.count(("AAAA", "TTTT")), 4)
    assertEquals(h.count(("AAAA", "TTTC")), 0)
  }

  property("track frequencies for arbitrary data") {
    forAll { (data: List[(Char, Char)]) =>
      val expectedCounts: Map[(Char, Char), Int] = data.groupBy(identity).view.mapValues(_.length).toMap

      val hist = new TupleHistogram[Char]
      data.foreach(hist.increment)

      expectedCounts.foreach { case (x, count) =>
        assertEquals(hist.count(x), count)
      }
    }
  }

}
