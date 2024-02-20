/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.{FileWriter, PrintWriter}
import java.nio.file.{Files, Path}

import scala.util.{Try, Using}

import cats.syntax.all.*
import org.broadinstitute.gpp.poolq3.hist.{ReadOnlyHistogram, ShardedHistogram}
import org.broadinstitute.gpp.poolq3.parser.BarcodeSet
import org.broadinstitute.gpp.poolq3.reference.Reference

object BarcodeCountsWriter:

  def write(
    file: Path,
    umiFileDir: Option[Path],
    hist: ShardedHistogram[String, (String, String)],
    rowReference: Reference,
    colReference: Reference,
    umiBarcodes: Option[BarcodeSet],
    dialect: ReportsDialect
  ): Try[Unit] =
    umiBarcodes match
      case None     => write(file, hist, rowReference, colReference, dialect)
      case Some(ub) => writeUmi(file, umiFileDir, hist, rowReference, colReference, ub, dialect)

  private[reports] def write(
    barcodeCountsFile: Path,
    hist: ReadOnlyHistogram[(String, String)],
    rowReference: Reference,
    colReference: Reference,
    dialect: ReportsDialect
  ): Try[Unit] =
    Using(new PrintWriter(new FileWriter(barcodeCountsFile.toFile))) { pw =>
      // write headers
      val colHeadings = colReference.allBarcodes.map(colReference.referenceBarcodeForDnaBarcode).mkString("\t")
      pw.println(countsHeaderText(dialect, colHeadings, rowReference.allBarcodes.size, colReference.allBarcodes.size))

      // write count data
      rowReference.allBarcodes.foreach { rowBc =>
        // write row identifiers
        writeRowIdentifiers(rowReference, rowBc, pw)

        // write row counts
        val columns: Seq[Int] = colReference.allBarcodes.map(colBc => hist.count((rowBc, colBc)))
        pw.println(columns.mkString("\t"))
      }
    }

  private[reports] def writeUmi(
    barcodeCountsFile: Path,
    umiFileDirOpt: Option[Path],
    hist: ShardedHistogram[String, (String, String)],
    rowReference: Reference,
    colReference: Reference,
    umiBarcodes: BarcodeSet,
    dialect: ReportsDialect
  ): Try[Unit] =
    val parsedFilename = parseFilename(barcodeCountsFile)
    val umiFileDir = umiFileDirOpt.getOrElse(barcodeCountsFile.resolveSibling("umi-barcode-counts"))
    val basename = parsedFilename.basename
    val extension = parsedFilename.extension
    write(barcodeCountsFile, hist, rowReference, colReference, dialect) *>
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

  end writeUmi

end BarcodeCountsWriter
