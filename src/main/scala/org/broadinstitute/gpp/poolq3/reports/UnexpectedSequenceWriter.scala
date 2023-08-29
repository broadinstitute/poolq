/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Path}

import scala.collection.mutable
import scala.io.Source
import scala.util.control.NonFatal
import scala.util.{Try, Using}

import it.unimi.dsi.fastutil.objects.{Object2IntOpenHashMap, Object2ObjectMap, Object2ObjectOpenHashMap}
import org.broadinstitute.gpp.poolq3.collection._
import org.broadinstitute.gpp.poolq3.reference.Reference
import org.log4s.{Logger, getLogger}

object UnexpectedSequenceWriter {

  private[this] val log: Logger = getLogger

  def write(
    outputFile: Path,
    unexpectedSequenceCacheDir: Path,
    sequencesToReport: Int,
    colReference: Reference,
    globalReference: Option[Reference]
  ): Try[Unit] = {
    val (h, r) = loadCache(unexpectedSequenceCacheDir, sequencesToReport)

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

  private[reports] def loadCache(
    cacheDir: Path,
    sequencesToReport: Int
  ): (Object2ObjectMap[String, Object2IntOpenHashMap[String]], Map[String, Int]) = {
    val h = new Object2ObjectOpenHashMap[String, Object2IntOpenHashMap[String]]()
    val r = new scala.collection.mutable.HashMap[String, Int]()

    cacheDir.toFile.listFiles().foreach { file =>
      log.debug(s"Processing ${file.getName}")
      parseFile(h, r, file)

      // now truncate the histograms to the top N
      truncateToN(h, r, sequencesToReport)
    }
    (h, r.toMap)
  }

  private[reports] def printUnexpectedCounts(
    colReference: Reference,
    globalReferenceOpt: Option[Reference],
    h: Object2ObjectMap[String, Object2IntOpenHashMap[String]],
    r: Map[String, Int],
    pw: PrintWriter
  ): Unit = {
    val colBarcodes = colReference.allBarcodes.map(colReference.referenceBarcodeForDnaBarcode)

    pw.println(headerText(colBarcodes))
    val rows = r.toSeq.sortBy { case (bc, count) => (-count, bc) }.map { case (k, _) => k }

    rows.foreach { rowBc =>
      val rowCounts = h.get(rowBc)
      val possibleIds =
        globalReferenceOpt.map(globalReference => globalReference.idsForBarcode(rowBc).mkString(",")).getOrElse("")
      val counts = colReference.allBarcodes.map(colBc => rowCounts.getInt(colBc))
      val total = counts.sum
      pw.println(s"$rowBc\t$total\t${counts.mkString("\t")}\t$possibleIds")
    }
  }

  private[reports] def headerText(colBarcodes: Seq[String]): String =
    s"Sequence\tTotal\t${colBarcodes.mkString("\t")}\tPotential IDs"

  private[reports] def parseFile(
    h: Object2ObjectMap[String, Object2IntOpenHashMap[String]],
    r: mutable.Map[String, Int],
    file: File
  ): Unit = {
    Using.resource(Source.fromFile(file)) { src =>
      src.getLines().zipWithIndex1.foreach { case (line: String, lineNo: Int) =>
        val fields = line.split(",", -1)
        if (fields.size != 2) {
          log.warn(s"Found ${fields.size} fields on line $lineNo of $file, expected 2")
        } else {
          val rowBc = fields(0)
          val colBc = fields(1)

          // can't avoid the double hash lookup here without a big hassle
          val _ = r.put(rowBc, r.getOrElseUpdate(rowBc, 0) + 1)
          h.putIfAbsent(rowBc, new Object2IntOpenHashMap[String]())
          h.get(rowBc).addTo(colBc, 1)
        }
      }
    }
  }

  private[reports] def truncateToN[A](
    h: Object2ObjectMap[A, Object2IntOpenHashMap[A]],
    r: mutable.Map[A, Int],
    n: Int
  ): Unit = {
    val drop = r.toSeq.sortBy { case (_, count) => -count }.drop(n)
    drop.foreach { case (bc, _) =>
      val _ = r.remove(bc)
      h.remove(bc)
    }
  }

}
