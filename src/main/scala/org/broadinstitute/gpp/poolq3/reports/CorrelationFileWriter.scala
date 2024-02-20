/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.PrintWriter
import java.nio.file.Path

import scala.util.{Success, Try, Using}

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import org.broadinstitute.gpp.poolq3.numeric.Decimal00Format
import org.broadinstitute.gpp.poolq3.reference.Reference
import org.broadinstitute.gpp.poolq3.reports.LogNormalizedCountsWriter.ColId
import org.broadinstitute.gpp.poolq3.types.CorrelationFileType
import org.log4s.{Logger, getLogger}

/** Computes and writes a condition-correlation matrix file. Compares counts for each experimental condition using
  * Pearson correlation.
  */
object CorrelationFileWriter:

  private[this] val log: Logger = getLogger

  def write(
    file: Path,
    normalizedCounts: Map[String, Map[ColId, Double]],
    rowReference: Reference,
    colReference: Reference
  ): Try[Option[CorrelationFileType.type]] =
    if colReference.allIds.size < 2 || rowReference.allBarcodes.size < 2 then
      log.warn(
        "Skipping correlation file for trivial dataset " +
          s"(${colReference.allIds.size} columns and ${rowReference.allBarcodes.size} rows)"
      )
      Success(None)
    else
      val cor = new PearsonsCorrelation()
      val countsMatrix = makeCountsMatrix(normalizedCounts, rowReference, colReference)
      val pearsonMatrix = cor.computeCorrelationMatrix(countsMatrix)

      Using(new PrintWriter(file.toFile)) { pw =>
        printHeaders(colReference, pw)
        for i <- colReference.allIds.indices do
          pw.print(colReference.allIds(i))
          for j <- colReference.allIds.indices do pw.print("\t" + Decimal00Format.format(pearsonMatrix.getEntry(i, j)))
          pw.println()
        Some(CorrelationFileType)
      }

  private[reports] def printHeaders(colReference: Reference, pw: PrintWriter): Unit =
    pw.println("\t" + colReference.allIds.mkString("\t"))

  private def makeCountsMatrix(
    counts: Map[String, Map[ColId, Double]],
    rowReference: Reference,
    colReference: Reference
  ): Array[Array[Double]] =
    val matrix = Array.ofDim[Double](rowReference.allBarcodes.size, colReference.allIds.size)

    for i <- rowReference.allBarcodes.indices do
      val row = rowReference.allBarcodes(i)
      val rowCounts = counts(row)

      for j <- colReference.allIds.indices do matrix(i)(j) = rowCounts(colReference.allIds(j))

    matrix

  end makeCountsMatrix

end CorrelationFileWriter
