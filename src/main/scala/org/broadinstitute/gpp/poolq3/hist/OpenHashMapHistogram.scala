/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.hist

import scala.jdk.CollectionConverters.*

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap

class OpenHashMapHistogram[A] extends Histogram[A]:

  private val hist: Object2LongOpenHashMap[A] = new Object2LongOpenHashMap[A]()
  hist.defaultReturnValue(0L)

  /** Increment the occurrences of key `k` */
  override def increment(k: A): Long = hist.addTo(k, 1L)

  /** Returns the number of occurrences of key `k` */
  override def count(k: A): Long = hist.getLong(k) // getOrDefault(k, 0)

  /** Returns the keys tracked in this histogram */
  override def keys: Set[A] = hist.keySet().asScala.toSet

end OpenHashMapHistogram
