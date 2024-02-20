/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.hist

import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.Prop.forAll

class ShardedHistogramTest extends FunSuite with ScalaCheckSuite:

  test("basic operations") {
    val h = new BasicShardedHistogram[String, String](new OpenHashMapHistogram)

    // increment a few things
    val _ = h.increment(None, "AAAA")

    val _ = h.increment(Some("CCCC"), "AAAA")
    val _ = h.increment(Some("CCCC"), "AAAA")
    val _ = h.increment(Some("CCCT"), "AAAA")

    val _ = h.increment(Some("CCCT"), "TTTT")

    // make sure the resulting histogram checks out
    assertEquals(h.shards, Set("CCCC", "CCCT"))
    assertEquals(h.keys, Set("AAAA", "TTTT"))
    assertEquals(h.count("AAAA"), 4)
    assertEquals(h.forShard(Some("CCCC")).count("AAAA"), 2)
    assertEquals(h.keys(None), Set("AAAA"))
    assertEquals(h.keys(Some("CCCC")), Set("AAAA"))
    assertEquals(h.keys(Some("CCCT")), Set("AAAA", "TTTT"))
  }

  property("track frequencies for arbitrary data") {
    def key(x: Int): Option[Int] = if x < 0 then None else Some(x)
    forAll { (data: List[(Int, Int)]) =>
      val actualHistogram = new BasicShardedHistogram[Int, Int](new OpenHashMapHistogram)
      data.foreach { case (shard, value) =>
        actualHistogram.increment(key(shard), value)
      }

      val dataByShard = data.groupMap(t => key(t._1))(_._2)
      val expectedShardedHistogram = dataByShard.map { case (k, vs) =>
        k -> vs.groupBy(identity).view.mapValues(_.length).toMap
      }
      expectedShardedHistogram.foreach { case (shard, expectedHistogramShard) =>
        val actualHistogramShard = actualHistogram.forShard(shard)
        assertEquals(actualHistogramShard.keys, expectedHistogramShard.keySet)
        expectedHistogramShard.foreach { case (x, count) =>
          assertEquals(actualHistogramShard.count(x), count)
        }
      }
    }
  }

end ShardedHistogramTest
