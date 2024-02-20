/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.hist

trait ReadOnlyHistogram[A]:

  /** Returns the number of occurrences of key `k` */
  def count(k: A): Int

  /** Returns the keys tracked in this histogram */
  def keys: Set[A]

  def toMap: scala.collection.immutable.Map[A, Int] = keys.map(k => k -> count(k)).toMap

end ReadOnlyHistogram

/** Simple representation of a mutable histogram */
trait Histogram[A] extends ReadOnlyHistogram[A]:

  /** Increment the occurrences of key `k` */
  def increment(k: A): Int
