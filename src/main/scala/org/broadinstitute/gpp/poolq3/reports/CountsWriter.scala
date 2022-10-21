/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.PrintWriter
import java.nio.file.{Files, Path}

import scala.util.{Try, Using}

import cats.syntax.all._
import org.broadinstitute.gpp.poolq3.hist.{ReadOnlyHistogram, ShardedHistogram}
import org.broadinstitute.gpp.poolq3.parser.BarcodeSet
import org.broadinstitute.gpp.poolq3.reference.Reference

object CountsWriter {

  def write(
    countsFile: Path,
    umiFileDir: Option[Path],
    hist: ShardedHistogram[String, (String, String)],
    rowReference: Reference,
    colReference: Reference,
    umiBarcodes: Option[BarcodeSet],
    dialect: ReportsDialect
  ): Try[Unit] =
    umiBarcodes match {
      case None     => write(countsFile, hist, rowReference, colReference, dialect)
      case Some(ub) => writeUmi(countsFile, umiFileDir, hist, rowReference, colReference, ub, dialect)
    }

  private[reports] def write(
    countsFile: Path,
    hist: ReadOnlyHistogram[(String, String)],
    rowReference: Reference,
    colReference: Reference,
    dialect: ReportsDialect
  ): Try[Unit] =
    Using(new PrintWriter(countsFile.toFile)) { pw =>
      // write headers
      val colHeadings = colReference.allIds.mkString("\t")
      pw.println(countsHeaderText(dialect, colHeadings, rowReference.allBarcodes.size, colReference.allIds.size))

      // write count data
      rowReference.allBarcodes.foreach { rowBc =>
        // write row identifiers
        writeRowIdentifiers(rowReference, rowBc, pw)

        // write counts
        val columns: Seq[Int] = colReference.allIds.map { colId =>
          colReference.barcodesForId(colId).map(colBc => hist.count((rowBc, colBc))).sum
        }
        pw.println(columns.mkString("\t"))
      }
    }

  private[reports] def writeUmi(
    countsFile: Path,
    umiFileDirOpt: Option[Path],
    hist: ShardedHistogram[String, (String, String)],
    rowReference: Reference,
    colReference: Reference,
    umiBarcodes: BarcodeSet,
    dialect: ReportsDialect
  ): Try[Unit] = {
    val parsedFilename = parseFilename(countsFile)
    val umiFileDir = umiFileDirOpt.getOrElse(countsFile.resolveSibling("umi-counts"))
    val basename = parsedFilename.basename
    val extension = parsedFilename.extension
    write(countsFile, hist, rowReference, colReference, dialect) *>
      Try(Files.createDirectories(umiFileDir)) *>
      write(
        umiFileDir.resolve(s"$basename-UNMATCHED-UMI${extension.getOrElse("")}"),
        hist.forShard(None),
        rowReference,
        colReference,
        dialect
      ) *>
      umiBarcodes.barcodes.toList.traverse_ { shard =>
        write(
          umiFileDir.resolve(s"$basename-$shard${extension.getOrElse("")}"),
          hist.forShard(Some(shard)),
          rowReference,
          colReference,
          dialect
        )
      }
  }

}
