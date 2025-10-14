/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.process

class BarcodeStats:

  private var minPos: Int = Int.MaxValue
  private var maxPos: Int = -1
  private var sum: Long = 0L
  private var found: Long = 0L

  def min: Int = minPos
  def max: Int = maxPos

  def notFound(totalReads: Long): Long = totalReads - found

  def avg: Option[Double] =
    if found < 1 then None
    else Some(sum / found.toDouble)

  def update(pos: Int): Unit =
    found += 1L
    minPos = math.min(minPos, pos)
    maxPos = math.max(maxPos, pos)
    sum += pos

  def minPosStr = if min == Int.MaxValue then "N/A" else min.toString
  def maxPosStr = if min < 0 then "N/A" else max.toString

end BarcodeStats
