/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.hist

import scala.collection.mutable

import it.unimi.dsi.fastutil.objects.{Object2LongOpenHashMap, Object2ObjectOpenHashMap}

class TupleHistogram[A] extends Histogram[(A, A)]:

  private val hist: Object2ObjectOpenHashMap[A, Object2LongOpenHashMap[A]] = new Object2ObjectOpenHashMap()

  /** Increment the occurrences of key `k` */
  override def increment(k: (A, A)): Long =
    val (fst, snd) = k
    val sndMap = hist.get(fst)
    if sndMap != null then sndMap.addTo(snd, 1L)
    else
      val newSndMap = new Object2LongOpenHashMap[A]()
      newSndMap.put(snd, 1L)
      hist.put(fst, newSndMap)
      1L

  end increment

  /** Returns the number of occurrences of key `k` */
  override def count(k: (A, A)): Long =
    val (fst, snd) = k
    val sndMap = hist.get(fst)
    if sndMap == null then 0
    else sndMap.getOrDefault(snd, 0L)

  /** Returns the keys tracked in this histogram */
  override def keys: Set[(A, A)] =

    val s: mutable.Set[(A, A)] = new mutable.HashSet()
    hist.forEach { (fst, snds) =>
      snds.keySet().forEach { snd =>
        val _ = s.add((fst, snd))
      }
    }
    s.toSet

  end keys

end TupleHistogram
