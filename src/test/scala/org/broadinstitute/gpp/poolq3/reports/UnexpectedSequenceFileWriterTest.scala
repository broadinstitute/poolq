/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

import it.unimi.dsi.fastutil.objects.{Object2IntOpenHashMap, Object2ObjectOpenHashMap}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*

class UnexpectedSequenceFileWriterTest extends AnyFlatSpec {

  "truncateToN" should "truncate the histograms to the top N elements" in {
    val h = new Object2ObjectOpenHashMap[Long, Object2IntOpenHashMap[Long]]()
    val r = mutable.Map[Long, Int]()

    r += 0L -> 9
    h.put(0L, new Object2IntOpenHashMap[Long]())

    r += 1L -> 455
    h.put(1L, new Object2IntOpenHashMap[Long]())

    r += 2L -> 89
    h.put(2L, new Object2IntOpenHashMap[Long]())

    r += 3L -> 2
    h.put(3L, new Object2IntOpenHashMap[Long]())

    r += 4L -> 83
    h.put(4L, new Object2IntOpenHashMap[Long]())

    UnexpectedSequenceWriter.truncateToN(h, r, 3)

    val _ = r.keySet should be(Set(1L, 2L, 4L))
    h.keySet().iterator().asScala.toSet should be(Set(1L, 2L, 4L))
  }

}
