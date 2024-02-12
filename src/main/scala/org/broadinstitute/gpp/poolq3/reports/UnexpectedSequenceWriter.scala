/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.{Closeable, PrintWriter}
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
    nSequencesToReport: Int,
    colReference: Reference,
    globalReference: Option[Reference],
    maxMapSize: Int = 10_000_000
  ): Try[Unit] = {
    // build a "reference set" - a set of unexpected barcodes that we will track exact counts for (read `samplePct`% of each shard)
    // load the whole cache, tracking only sequences in the reference set
    val (h, r) = loadCache(unexpectedSequenceCacheDir, colReference, nSequencesToReport, maxMapSize)

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

  // defining this as a trait is not useful in the main codebase but it makes testing easier
  private[reports] trait CachedBarcodes extends Iterator[String] with Closeable {
    def colBc: String
  }

  final private[reports] class SourceCachedBarcodes(val colBc: String, source: Source) extends CachedBarcodes {
    private val iter = source.getLines()
    def close(): Unit = source.close()
    def hasNext: Boolean = iter.hasNext
    def next(): String = iter.next()
  }

  final private[reports] class BreadthFirstIterator(readers: mutable.ArrayBuffer[CachedBarcodes])
      extends Iterator[(String, String)] {
    private var i = 0

    // this only works if all the readers were non-empty at the start
    def hasNext: Boolean = readers.nonEmpty && readers(i).hasNext

    def next(): (String, String) = {
      val reader = readers(i)
      val ret = (reader.next(), reader.colBc)

      // if this reader won't produce another barcode after this, remove it
      if (!reader.hasNext) {
        val removed = readers.remove(i)
        removed.close()
        // in this case, we don't advance `i` because we've removed the current `i`, so `i`
        // points to the next reader by virtue of the changed dat astructure
        if (readers.nonEmpty) {
          i = i % readers.length
        }
      } else if (readers.nonEmpty) {
        // if readers is empty, `readers.length` will be 0 and % divides by zero; advancing `i` is only
        // necessary if we can read more from the buffer anyway
        i = (i + 1) % readers.length
      }

      ret
    }

  }

  def loadCache(
    cacheDir: Path,
    colReference: Reference,
    nSequencesToReport: Int,
    maxMapSize: Int
  ): (Map[String, Map[String, Int]], Vector[String]) = {
    val rowColBarcodeCounts = new mutable.HashMap[String, mutable.Map[String, Int]]()
    val allRowBarcodeCounts = new mutable.HashMap[String, Int]()

    // create & populate the list of readers
    val readers = mutable.ArrayBuffer[CachedBarcodes]()
    try {
      colReference.allBarcodes.foreach { colBc =>
        val file = cacheDir.resolve(nameFor(colBc))
        if (Files.exists(file)) {
          val cbc = new SourceCachedBarcodes(colBc, Source.fromFile(file.toFile))
          if (cbc.hasNext) {
            readers.addOne(cbc)
          }
        }
      }

      val iterator = new BreadthFirstIterator(readers)

      while (rowColBarcodeCounts.keySet.size < maxMapSize && iterator.hasNext) {
        val (rowBc, colBc) = iterator.next()
        val colBarcodeMap = rowColBarcodeCounts.getOrElseUpdate(rowBc, mutable.HashMap())
        val _ = colBarcodeMap.updateWith(colBc) {
          case None    => Some(1)
          case Some(c) => Some(c + 1)
        }
        val _ = allRowBarcodeCounts.updateWith(rowBc) {
          case None    => Some(1)
          case Some(c) => Some(c + 1)
        }
      }

      // at this point, we either exhausted the readers or we filled the map; go through the remaining data
      // and tally things up, but don't add new keys to the outer map
      readers.foreach { rdr =>
        val colBc = rdr.colBc
        rdr.foreach { rowBc =>
          // now, we only update if there was an existing entry in `rowColBarcodeCounts` because it means it's
          // in the set of things we're keeping track of
          rowColBarcodeCounts.get(rowBc).foreach { colBarcodeMap =>
            val _ = colBarcodeMap.updateWith(colBc) {
              case None    => Some(1)
              case Some(c) => Some(c + 1)
            }
            val _ = allRowBarcodeCounts.updateWith(rowBc) {
              case None    => Some(1)
              case Some(c) => Some(c + 1)
            }
          }
        }
        rdr.close()
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
    } finally {
      readers.foreach(_.close())
    }
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
