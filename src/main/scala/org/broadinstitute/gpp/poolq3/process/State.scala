/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
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
) {

  var reads: Int = 0
  var exactMatches: Int = 0
  var matches: Int = 0
  var neitherRowBarcodeFound: Int = 0
  def rowBarcodeNotFound: Int = rowBarcodeStats.notFound(reads)
  def revRowBarcodeNotFound: Int = revRowBarcodeStats.notFound(reads)
  var umiBarcodeNotFound: Int = 0
  val rowBarcodeStats: BarcodeStats = new BarcodeStats
  val revRowBarcodeStats: BarcodeStats = new BarcodeStats

  def matchPercent: Double =
    if reads < 1 then 0.0
    else 100L * matches / reads.toDouble

}
