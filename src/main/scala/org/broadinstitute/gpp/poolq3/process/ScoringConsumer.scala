/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.process

import java.nio.file.Path
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import org.broadinstitute.gpp.poolq3.barcode.{Barcodes, FoundBarcode}
import org.broadinstitute.gpp.poolq3.hist.{BasicShardedHistogram, OpenHashMapHistogram, TupleHistogram}
import org.broadinstitute.gpp.poolq3.parser.BarcodeSet
import org.broadinstitute.gpp.poolq3.reference.{MatchedBarcode, Reference}
import org.broadinstitute.gpp.poolq3.seq._
import org.log4s.{Logger, getLogger}

final class ScoringConsumer(
  rowReference: Reference,
  colReference: Reference,
  countAmbiguous: Boolean,
  alwaysCountColumnBarcodes: Boolean,
  umiReference: Option[BarcodeSet],
  unexpectedReportCache: Option[Path],
  pairedEndMode: Boolean
) extends Consumer {

  private[this] val log: Logger = getLogger

  private[this] val unexpectedSequenceQueue: ArrayBlockingQueue[(Array[Char], Array[Char])] =
    new ArrayBlockingQueue(1000)

  private[this] val unexpectedSequenceTrackerOpt: Option[UnexpectedSequenceTracker] =
    unexpectedReportCache.map(new UnexpectedSequenceTracker(_))

  // used to tell the unexpected sequence tracker thread when processing is done
  @volatile private[this] var done = false

  // tracks the state as we go
  override val state =
    new State(
      new BasicShardedHistogram[String, (String, String)](new TupleHistogram()),
      new OpenHashMapHistogram(),
      new OpenHashMapHistogram(),
      new OpenHashMapHistogram()
    )

  // this thread is used to write unexpected sequences to the file cache
  private[this] val unexpectedSequenceTrackerThread: Thread = new Thread {

    final override def run(): Unit = {
      assert(unexpectedSequenceTrackerOpt.isDefined)
      val unexpectedSequenceTracker = unexpectedSequenceTrackerOpt.get
      while (!done || !unexpectedSequenceQueue.isEmpty) {
        try {
          Option(unexpectedSequenceQueue.poll(100, TimeUnit.MILLISECONDS))
            .foreach(unexpectedSequenceTracker.reportUnexpected)
        } catch {
          case _: InterruptedException =>
            log.debug(s"Interrupted. Done = $done; queue length = ${unexpectedSequenceQueue.size()}")
        }
      }
    }

  }

  override def start(): Unit = {
    unexpectedReportCache.foreach(_ => unexpectedSequenceTrackerThread.start())
  }

  override def close(): Unit =
    unexpectedReportCache.foreach { _ =>
      done = true
      unexpectedSequenceTrackerThread.join()
      unexpectedSequenceTrackerOpt.foreach(_.close())
    }

  override def consume(parsedBarcode: Barcodes): Unit = {
    // increment the read counter regardless
    state.reads += 1

    (parsedBarcode.row, parsedBarcode.revRow, parsedBarcode.col) match {
      case (f @ Some(_), revRowOpt, None) =>
        // a forward row barcode region was found; extract the sequence and update stats
        updateRowBarcodePositionStats(f, if (pairedEndMode) revRowOpt else None)

      case (f @ Some(parsedRow), None, Some(parsedCol)) =>
        updateRowBarcodePositionStats(f, None)
        if (pairedEndMode == false) {
          // a row barcode region was found; extract the sequence and update stats

          // match the sequence against the row reference to determine if this was a known barcode
          val rowBc: Seq[MatchedBarcode] = rowReference.find(parsedRow.barcode)
          val colBc: Seq[MatchedBarcode] = colReference.find(parsedCol.barcode)

          // handle the case where we matched a column at least
          updateColumnBarcodeStats(colBc, parsedCol)

          // for every column and reference barcode matched by this read, increment the co-occurrence count, overall
          // match count, and potentially, the exact match count
          rowBc.foreach(row => colBc.foreach(col => matchedRowAndCol(row, col, parsedBarcode.umi)))

          // if we are tracking unexpected sequences and we matched the column barcode to the reference data but didn't
          // match the row barcode to the reference data, and the row barcode doesn't have an N in it, then queue the
          // row barcode for inclusion in the unexpected sequence report
          if (unexpectedReportCache.isDefined && colBc.nonEmpty && rowBc.isEmpty && !containsN(parsedRow.barcode)) {
            unexpectedSequenceQueue.put((parsedRow.barcode, parsedCol.barcode))
          }
        }

      case (f @ Some(parsedRow), r @ Some(parsedRevRow), Some(parsedCol)) =>
        // a row barcode region was found; extract the sequence and update stats
        updateRowBarcodePositionStats(f, r)

        val combinedBarcode = Array.concat(parsedRow.barcode, parsedRevRow.barcode)

        // match the sequence against the row reference to determine if this was a known barcode
        val rowBc: Seq[MatchedBarcode] = rowReference.find(combinedBarcode)
        val colBc: Seq[MatchedBarcode] = colReference.find(parsedCol.barcode)

        // handle the case where we matched a column at least
        updateColumnBarcodeStats(colBc, parsedCol)

        // for every column and reference barcode matched by this read, increment the co-occurrence count, overall
        // match count, and potentially, the exact match count
        rowBc.foreach(row => colBc.foreach(col => matchedRowAndCol(row, col, parsedBarcode.umi)))

        // if we are tracking unexpected sequences and we matched the column barcode to the reference data but didn't
        // match the row barcode to the reference data, and the row barcode doesn't have an N in it, then queue the
        // row barcode for inclusion in the unexpected sequence report
        if (unexpectedReportCache.isDefined && colBc.nonEmpty && rowBc.isEmpty && !containsN(parsedRow.barcode)) {
          unexpectedSequenceQueue.put((parsedRow.barcode, parsedCol.barcode))
        }

      case (None, r, None) =>
        updateRowBarcodePositionStats(None, r)

      case (None, r, Some(col)) =>
        updateRowBarcodePositionStats(None, r)

        // however, by default, we do NOT update the column barcode counts; this is a difference between PoolQ3 and
        // PoolQ2 that has been agreed upon by TG, MT, and JD
        // the thinking is this: if the column barcode is found and recognized but a row barcode cannot be found at all
        // in that portion of the read, the read quality is suspect (e.g., a potential primer-dimer) and should not be
        // counted; instead, we're counting sample barcode matches only when the rest of the read matches to the
        // expected structure
        if (alwaysCountColumnBarcodes) {
          val colBc: Seq[MatchedBarcode] = colReference.find(col.barcode)
          updateColumnBarcodeStats(colBc, col)
        }
    }

  }

  // Process the row and column barcodes when both are found and match to reference data
  private[this] def matchedRowAndCol(row: MatchedBarcode, col: MatchedBarcode, umi: Option[FoundBarcode]): Unit = {
    val r = row.barcode
    val c = col.barcode
    log.debug(s"Incrementing state for ($r, $c}).")
    umiReference match {
      case None =>
        None
        // we're not in UMI mode, so just increment the state
        state.known.increment(None, (r, c))
      case Some(ref) =>
        // we're in UMI mode
        handleUmi(umi, ref, r, c)
    }
    state.matches += 1
    if (row.distance == 0) {
      state.exactMatches += 1
    }
  }

  // Process a UMI barcode if we're doing that
  private[this] def handleUmi(umi: Option[FoundBarcode], ref: BarcodeSet, r: String, c: String): Unit = {
    umi match {
      case Some(s) =>
        val u = new String(s.barcode)
        if (ref.isDefined(u)) {
          // we found a known UMI barcode, so increment
          val _ = state.known.increment(Some(u), (r, c))
        } else {
          // we found an unknown UMI barcode, so track it somehow
          state.known.increment(None, (r, c))
          val _ = state.unknownUmi.increment(u)
        }
      case None =>
        // this means we were configured for UMI but we didn't extract a UMI barcode at all
        state.umiBarcodeNotFound += 1
    }
  }

  private[this] def updateColumnBarcodeStats(colBc: Seq[MatchedBarcode], col: FoundBarcode): Unit = {
    // the if and else if branches aren't really related but they are also mutually exclusive, so if one matches
    // there is no reason to test the other
    if (countAmbiguous || colBc.lengthCompare(1) == 0) {
      colBc.foreach(mb => state.knownCol.increment(mb.barcode))
    } else if (colBc.isEmpty && !containsN(col.barcode)) {
      val _ = state.unknownCol.increment(new String(col.barcode))
    }
  }

  private[this] def updateRowBarcodePositionStats(row: Option[FoundBarcode], revRow: Option[FoundBarcode]): Unit = {
    row.foreach(r => state.rowBarcodeStats.update(r.offset0))
    revRow.foreach(r => state.revRowBarcodeStats.update(r.offset0))
    if (row.isEmpty && revRow.isEmpty) { state.neitherRowBarcodeFound += 1 }
  }

  override def readsProcessed: Int = state.reads

  override def matchingReads: Int = state.matches

  override def matchPercent: Float = state.matchPercent.toFloat

}
