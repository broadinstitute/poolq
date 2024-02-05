/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.PrintWriter
import java.nio.file.{Files, Path}

import scala.collection.mutable
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.{Try, Using}

import org.broadinstitute.gpp.poolq3.process.UnexpectedSequenceTracker.nameFor
import org.broadinstitute.gpp.poolq3.reference.Reference
import org.log4s.{Logger, getLogger}

object UnexpectedSequenceWriter {

  private[this] val log: Logger = getLogger

  def write(
    outputFile: Path,
    unexpectedSequenceCacheDir: Path,
    unexpectedBarcodeCounts: Map[String, Int],
    nSequencesToReport: Int,
    colReference: Reference,
    globalReference: Option[Reference],
    samplePct: Float
  ): Try[Unit] = {
    // build a "reference set" - a set of unexpected barcodes that we will track exact counts for (read `samplePct`% of each shard)
    val sequencesToReport = sampleCache(unexpectedSequenceCacheDir, samplePct, colReference, unexpectedBarcodeCounts)
    // load the whole cache, tracking only sequences in the reference set
    val (h, r) = loadCache(unexpectedSequenceCacheDir, colReference, sequencesToReport, nSequencesToReport)

    Using(new PrintWriter(outputFile.toFile))(pw => printUnexpectedCounts(colReference, globalReference, h, r, pw))
  }

  def removeCache(unexpectedSequenceCacheDir: Path): Unit = {
    // swallow non-fatal exceptions
    def tryDelete(p: Path): Unit =
      try
        Files.delete(p)
      catch {
        case NonFatal(_) => log.warn(s"Unable to delete ${p.toAbsolutePath}")
      }

    // run for side effects
    Files.list(unexpectedSequenceCacheDir).forEach(tryDelete)
    tryDelete(unexpectedSequenceCacheDir)
  }

  // builds the set of unexpected sequences we will actually count data for
  private[reports] def sampleCache(
    cacheDir: Path,
    samplePct: Float,
    colReference: Reference,
    unexpectedCountsByBarcode: Map[String, Int]
  ): Set[String] = {
    // this set will contain the barcodes to track for all shards
    val ret = mutable.HashSet[String]()

    // read the shard for each column barcode in turn
    colReference.allBarcodes.foreach { dnaBarcode =>
      // compute the number of lines to read for this shard by the samplePct and the number of reads we got for it
      val linesToRead: Int =
        math.ceil((unexpectedCountsByBarcode.getOrElse(dnaBarcode, 0) * samplePct).toDouble).toInt
      val file = cacheDir.resolve(nameFor(dnaBarcode))
      if (Files.exists(file)) {
        // add all the row barcodes found in the first `linesToRead` lines to `ret`
        Using.resource(Source.fromFile(file.toFile)) { src =>
          src.getLines().take(linesToRead).foreach(line => ret += line)
        }
      } else {
        log.info(s"No unexpected cache file found for $dnaBarcode")
      }
    }
    ret.toSet
  }

  private[reports] def loadCache(
    cacheDir: Path,
    colReference: Reference,
    sequencesToReport: Set[String],
    nSequencesToReport: Int
  ): (Map[String, Map[String, Int]], Vector[String]) = {
    val rowColBarcodeCounts = new mutable.HashMap[String, mutable.Map[String, Int]]()
    val allRowBarcodeCounts = new mutable.HashMap[String, Int]()

    colReference.allBarcodes.foreach { colBc =>
      val rowCountMap = parseFile(cacheDir, colBc, sequencesToReport)

      rowCountMap.foreach { case (rowBc, count) =>
        val colBarcodeCounts = rowColBarcodeCounts.getOrElseUpdate(rowBc, mutable.HashMap[String, Int]())
        val _ = colBarcodeCounts.updateWith(colBc) {
          case None                 => Some(count)
          case Some(prevColBcCount) => Some(prevColBcCount + count)
        }
        allRowBarcodeCounts.updateWith(rowBc) {
          case None     => Some(count)
          case Some(pc) => Some(pc + count)
        }
      }
    }

    // find the most popular N row barcodes
    val mostCommonRowBarcodesRanked = allRowBarcodeCounts.toVector.sortBy(-_._2).take(nSequencesToReport).map(_._1)
    val mostCommonRowBarcodes = mostCommonRowBarcodesRanked.toSet

    // filter out everything else and convert to an immutable map
    (
      rowColBarcodeCounts
        .filterInPlace { case (rowBc, _) => mostCommonRowBarcodes(rowBc) }
        .view
        .mapValues(m => m.toMap)
        .toMap,
      mostCommonRowBarcodesRanked
    )
  }

  // n.b. returns a mutable map from row barcode to counts
  private[reports] def parseFile(
    cacheDir: Path,
    colBc: String,
    sequencesToReport: Set[String]
  ): mutable.Map[String, Int] = {
    val r = mutable.HashMap[String, Int]()
    val file = cacheDir.resolve(nameFor(colBc))
    if (Files.exists(file)) {
      log.debug(s"Processing ${file.getName}")
      Using.resource(Source.fromFile(file.toFile)) { src =>
        src.getLines().foreach { rowBc =>
          // can't avoid the double hash lookup here without a big hassle
          if (sequencesToReport.contains(rowBc)) {
            r.updateWith(rowBc) {
              case None    => Some(1)
              case Some(i) => Some(i + 1)
            }
          }
        }
      }
    }
    r
  }

  private[reports] def printUnexpectedCounts(
    colReference: Reference,
    globalReferenceOpt: Option[Reference],
    h: Map[String, Map[String, Int]],
    rows: Vector[String],
    pw: PrintWriter
  ): Unit = {
    val colBarcodes = colReference.allBarcodes.map(colReference.referenceBarcodeForDnaBarcode)

    pw.println(headerText(colBarcodes))

    rows.foreach { rowBc =>
      val rowCounts = h.getOrElse(rowBc, Map.empty)
      val possibleIds =
        globalReferenceOpt.map(globalReference => globalReference.idsForBarcode(rowBc).mkString(",")).getOrElse("")
      val counts = colReference.allBarcodes.map(colBc => rowCounts.getOrElse(colBc, 0))
      val total = counts.sum
      pw.println(s"$rowBc\t$total\t${counts.mkString("\t")}\t$possibleIds")
    }
  }

  private[reports] def headerText(colBarcodes: Seq[String]): String =
    s"Sequence\tTotal\t${colBarcodes.mkString("\t")}\tPotential IDs"

}
