/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
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

object UnexpectedSequenceWriter:

  private[this] val log: Logger = getLogger

  def write(
    outputFile: Path,
    unexpectedSequenceCacheDir: Path,
    nSequencesToReport: Int,
    colReference: Reference,
    globalReference: Option[Reference],
    maxMapSize: Int = 10_000_000
  ): Try[Unit] =
    // build a "reference set" - a set of unexpected barcodes that we will track exact counts for (read `samplePct`% of each shard)
    // load the whole cache, tracking only sequences in the reference set
    val (h, r) = loadCache(unexpectedSequenceCacheDir, colReference, nSequencesToReport, maxMapSize)

    Using(new PrintWriter(outputFile.toFile))(pw => printUnexpectedCounts(colReference, globalReference, h, r, pw))

  end write

  def removeCache(unexpectedSequenceCacheDir: Path): Unit =
    // swallow non-fatal exceptions
    def tryDelete(p: Path): Unit =
      try Files.delete(p)
      catch case NonFatal(_) => log.warn(s"Unable to delete ${p.toAbsolutePath}")

    // run for side effects
    Files.list(unexpectedSequenceCacheDir).forEach(tryDelete)
    tryDelete(unexpectedSequenceCacheDir)

  end removeCache

  // defining this as a trait is not useful in the main codebase but it makes testing easier
  private[reports] trait CachedBarcodes extends Iterator[String] with Closeable:
    def colBc: String

  final private[reports] class SourceCachedBarcodes(val colBc: String, source: Source) extends CachedBarcodes:
    private val iter = source.getLines()
    def close(): Unit = source.close()
    def hasNext: Boolean = iter.hasNext
    def next(): String = iter.next()

  final private[reports] class BreadthFirstIterator(readers: mutable.Queue[CachedBarcodes])
      extends Iterator[(String, String)]:
    def hasNext: Boolean = readers.nonEmpty

    def next(): (String, String) =
      val reader = readers.dequeue()
      val ret = (reader.next(), reader.colBc)
      if reader.hasNext then readers.enqueue(reader)
      else reader.close()
      ret

  end BreadthFirstIterator

  def loadCache(
    cacheDir: Path,
    colReference: Reference,
    nSequencesToReport: Int,
    maxMapSize: Int
  ): (Map[String, Map[String, Int]], Vector[String]) =
    val rowColBarcodeCounts = new mutable.HashMap[String, mutable.Map[String, Int]]()
    val allRowBarcodeCounts = new mutable.HashMap[String, Int]()

    // create & populate the list of readers
    val readers = mutable.Queue[CachedBarcodes]()
    try
      colReference.allBarcodes.foreach { colBc =>
        val file = cacheDir.resolve(nameFor(colBc))
        if Files.exists(file) then
          val cbc = new SourceCachedBarcodes(colBc, Source.fromFile(file.toFile))
          if cbc.hasNext then readers.enqueue(cbc)
      }

      val iterator = new BreadthFirstIterator(readers)

      while rowColBarcodeCounts.size < maxMapSize && iterator.hasNext do
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
      end while
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
    finally readers.foreach(_.close())

    end try

  end loadCache

  private[reports] def printUnexpectedCounts(
    colReference: Reference,
    globalReferenceOpt: Option[Reference],
    h: Map[String, Map[String, Int]],
    rows: Vector[String],
    pw: PrintWriter
  ): Unit =
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

  end printUnexpectedCounts

  private[reports] def headerText(colBarcodes: Seq[String]): String =
    s"Sequence\tTotal\t${colBarcodes.mkString("\t")}\tPotential IDs"

end UnexpectedSequenceWriter
