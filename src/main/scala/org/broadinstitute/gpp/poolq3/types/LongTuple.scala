/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.types

import it.unimi.dsi.fastutil.HashCommon

final class LongTuple(val _1: Long, val _2: Long):

  override def equals(other: Any): Boolean =
    other match
      case that: LongTuple if this._1 == that._1 && this._2 == that._2 => true
      case _                                                           => false

  override def hashCode(): Int =
    val h1 = HashCommon.murmurHash3(_1).toInt
    val h2 = HashCommon.murmurHash3(_2).toInt
    (h1 & 0x7fffffff) * 16661 + (h2 & 0x7fffffff)

end LongTuple

object LongTuple:

  def apply(l1: Long, l2: Long): LongTuple = new LongTuple(l1, l2)

  def unapply(lt: LongTuple): Option[(Long, Long)] = Some((lt._1, lt._2))
