/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.hist

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

trait ShardedHistogram[A, B] extends ReadOnlyHistogram[B]:

  def shards: Set[A]

  def forShard(shard: Option[A]): Histogram[B]

  def increment(shard: Option[A], k: B): Int

  def keys(shard: Option[A]): Set[B]

  override def keys: Set[B]

  override def count(k: B): Int =
    var ret = 0
    shards.foreach(s => ret += forShard(Some(s)).count(k))
    ret += forShard(None).count(k)
    ret

end ShardedHistogram

class BasicShardedHistogram[A, B](make: => Histogram[B]) extends ShardedHistogram[A, B]:

  val hs: Object2ObjectOpenHashMap[A, Histogram[B]] = new Object2ObjectOpenHashMap[A, Histogram[B]]()
  val nullShard: Histogram[B] = make

  override def forShard(shard: Option[A]): Histogram[B] =
    shard match
      case None => nullShard
      case Some(s) => hs.compute(s, (_, h) => if h == null then make else h)

  override def increment(shard: Option[A], k: B): Int = forShard(shard).increment(k)

  override def shards: Set[A] = hs.keySet().asScala.toSet

  override def keys(shard: Option[A]): Set[B] = forShard(shard).keys

  override def keys: Set[B] =
    val keys: mutable.Set[B] = new mutable.HashSet
    keys.addAll(nullShard.keys)
    hs.forEach((_, h) => keys.addAll(h.keys))
    keys.toSet

end BasicShardedHistogram
