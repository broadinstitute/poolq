/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.hist

import scala.jdk.CollectionConverters.*

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

class OpenHashMapHistogram[A] extends Histogram[A]:

  private[this] val hist: Object2IntOpenHashMap[A] = new Object2IntOpenHashMap[A]()
  hist.defaultReturnValue(0)

  /** Increment the occurrences of key `k` */
  override def increment(k: A): Int = hist.addTo(k, 1)

  /** Returns the number of occurrences of key `k` */
  override def count(k: A): Int = hist.getInt(k) // getOrDefault(k, 0)

  /** Returns the keys tracked in this histogram */
  override def keys: Set[A] = hist.keySet().asScala.toSet

end OpenHashMapHistogram
