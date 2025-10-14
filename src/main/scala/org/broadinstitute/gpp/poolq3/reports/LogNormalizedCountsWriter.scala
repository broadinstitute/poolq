/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.PrintWriter
import java.nio.file.Path

import scala.util.{Try, Using}

import org.broadinstitute.gpp.poolq3.hist.ReadOnlyHistogram
import org.broadinstitute.gpp.poolq3.numeric.logNormalize
import org.broadinstitute.gpp.poolq3.reference.Reference

object LogNormalizedCountsWriter:

  type ColId = String

  def write(
      file: Path,
      counts: Map[String, Map[ColId, Double]],
      rowReference: Reference,
      colReference: Reference,
      dialect: ReportsDialect
  ): Try[Unit] =
    Using(new PrintWriter(file.toFile)) { pw =>
      val colHeadings = colReference.allIds.mkString("\t")

      pw.println(countsHeaderText(dialect, colHeadings, rowReference.allBarcodes.size, colReference.allBarcodes.size))

      rowReference.allBarcodes.foreach { rowBc =>
        // write row identifiers
        writeRowIdentifiers(rowReference, rowBc, pw)

        val rowCounts = counts(rowBc)
        val columns = colReference.allIds.map(rowCounts)
        pw.println(columns.mkString("\t"))
      }
    }

  def logNormalizedCounts(
      hist: ReadOnlyHistogram[(String, String)],
      rowReference: Reference,
      colReference: Reference
  ): Map[String, Map[ColId, Double]] =
    val columnReadCounts: Map[String, Long] = getColumnReadCounts(rowReference, colReference, hist)

    rowReference.allBarcodes.map { row =>
      val columns = colReference.allIds.map { colId =>
        val coocurrenceCount =
          colReference
            .barcodesForId(colId)
            .map(col => hist.count((row, col)))
            .sum
        val columnCount = columnReadCounts.getOrElse(colId, 0L)
        colId -> logNormalize(coocurrenceCount, columnCount)
      }.toMap

      row -> columns
    }.toMap

  end logNormalizedCounts

end LogNormalizedCountsWriter
