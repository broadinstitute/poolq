/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.PrintWriter
import java.nio.file.Path

import scala.util.{Try, Using}

import org.broadinstitute.gpp.poolq3.process.State

object UmiQualityWriter:

  def write(file: Path, state: State): Try[Unit] =
    // aggregate report data
    val umiBarcodeFrequencies = state.known.shards.toList.sorted.map { shard =>
      val hist = state.known.forShard(Some(shard))
      val count = hist.keys.map(hist.count).sum
      shard -> count
    }
    val unexpectedBarcodeFrequencies =
      state.unknownUmi.keys.map(k => BarcodeFrequency(k, state.unknownUmi.count(k))).toList
    val topNUnexpectedBarcodeFrequencies = topN(unexpectedBarcodeFrequencies, 100)

    // write it
    Using(new PrintWriter(file.toFile)) { pw =>
      pw.println("UMI Barcode\tCount")
      umiBarcodeFrequencies.foreach { case (shard, count) => pw.println(s"$shard\t$count") }
      pw.println()
      pw.println("Unexpected UMI Barcode\tCount")
      topNUnexpectedBarcodeFrequencies.foreach { case BarcodeFrequency(bc, count) => pw.println(s"$bc\t$count") }
      pw.println()
    }

  end write

end UmiQualityWriter
