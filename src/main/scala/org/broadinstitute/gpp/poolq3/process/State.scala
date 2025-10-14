/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.process

import org.broadinstitute.gpp.poolq3.hist.{Histogram, ShardedHistogram}

final class State(
    val known: ShardedHistogram[String, (String, String)],
    val knownCol: Histogram[String],
    val unknownCol: Histogram[String],
    val unknownUmi: Histogram[String]
):

  var reads: Long = 0L
  var exactMatches: Long = 0L
  var matches: Long = 0L
  var neitherRowBarcodeFound: Long = 0L
  def rowBarcodeNotFound: Long = rowBarcodeStats.notFound(reads)
  def revRowBarcodeNotFound: Long = revRowBarcodeStats.notFound(reads)
  var umiBarcodeNotFound: Long = 0L
  val rowBarcodeStats: BarcodeStats = new BarcodeStats
  val revRowBarcodeStats: BarcodeStats = new BarcodeStats

  def matchPercent: Double =
    if reads < 1 then 0.0
    else 100L * matches / reads.toDouble

end State
