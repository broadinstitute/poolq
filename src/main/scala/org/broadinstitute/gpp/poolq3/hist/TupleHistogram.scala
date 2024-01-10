/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.hist

import scala.collection.mutable

import it.unimi.dsi.fastutil.objects.{Object2IntOpenHashMap, Object2ObjectOpenHashMap}

class TupleHistogram[A] extends Histogram[(A, A)] {

  private[this] val hist: Object2ObjectOpenHashMap[A, Object2IntOpenHashMap[A]] = new Object2ObjectOpenHashMap()

  /** Increment the occurrences of key `k` */
  override def increment(k: (A, A)): Int = {
    val (fst, snd) = k
    val sndMap = hist.get(fst)
    if sndMap != null then sndMap.addTo(snd, 1)
    else {
      val newSndMap = new Object2IntOpenHashMap[A]()
      newSndMap.put(snd, 1)
      hist.put(fst, newSndMap)
      1
    }
  }

  /** Returns the number of occurrences of key `k` */
  override def count(k: (A, A)): Int = {
    val (fst, snd) = k
    val sndMap = hist.get(fst)
    if sndMap == null then 0
    else sndMap.getOrDefault(snd, 0)
  }

  /** Returns the keys tracked in this histogram */
  override def keys: Set[(A, A)] = {

    val s: mutable.Set[(A, A)] = new mutable.HashSet()
    hist.forEach { (fst, snds) =>
      snds.keySet().forEach { snd =>
        val _ = s.add((fst, snd))
      }
    }
    s.toSet
  }

}
