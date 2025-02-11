/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.PrintWriter
import java.nio.file.Path

import scala.util.{Try, Using}

import org.broadinstitute.gpp.poolq3.numeric.{Decimal000Format, Decimal00Format, logNormalize, percent}
import org.broadinstitute.gpp.poolq3.process.State
import org.broadinstitute.gpp.poolq3.reference.Reference

object QualityWriter:

  class TeeWriter(w1: PrintWriter, w2: PrintWriter):

    def print(s: String): Unit =
      w1.print(s)
      w2.print(s)

    def println(s: String): Unit =
      w1.println(s)
      w2.println(s)

    def println(): Unit =
      w1.println()
      w2.println()

  end TeeWriter

  def write(
      qualityFile: Path,
      conditionBarcodeCountsSummaryFile: Path,
      state: State,
      rowReference: Reference,
      colReference: Reference,
      isPairedEnd: Boolean
  ): Try[Unit] =
    Try {
      Using.resources(new PrintWriter(qualityFile.toFile), new PrintWriter(conditionBarcodeCountsSummaryFile.toFile)) {
        case (qualityWriter, cbcsWriter) =>
          val barcodeLocationStats =
            if isPairedEnd then
              s"""Reads with no construct barcode: ${state.rowBarcodeNotFound + state.revRowBarcodeNotFound - state.neitherRowBarcodeFound}
             |
             |Reads with no forward construct barcode: ${state.rowBarcodeNotFound}
             |Max forward construct barcode index: ${state.rowBarcodeStats.maxPosStr}
             |Min forward construct barcode index: ${state.rowBarcodeStats.minPosStr}
             |Avg forward construct barcode index: ${decOptFmt(state.rowBarcodeStats.avg)}
             |
             |Reads with no reverse construct barcode: ${state.revRowBarcodeNotFound}
             |Max reverse construct barcode index: ${state.revRowBarcodeStats.maxPosStr}
             |Min reverse construct barcode index: ${state.revRowBarcodeStats.minPosStr}
             |Avg reverse construct barcode index: ${decOptFmt(state.revRowBarcodeStats.avg)}""".stripMargin
            else s"""Reads with no construct barcode: ${state.rowBarcodeNotFound}
             |Max construct barcode index: ${state.rowBarcodeStats.maxPosStr}
             |Min construct barcode index: ${state.rowBarcodeStats.minPosStr}
             |Avg construct barcode index: ${decOptFmt(state.rowBarcodeStats.avg)}""".stripMargin

          val header =
            s"""Total reads: ${state.reads}
           |Matching reads: ${state.matches}
           |1-base mismatch reads: ${state.matches - state.exactMatches}
           |
           |Overall % match: ${Decimal00Format.format(state.matchPercent)}
           |
           |$barcodeLocationStats
           |""".stripMargin

          qualityWriter.println(header)

          qualityWriter.println(s"Read counts for sample barcodes with associated conditions:")

          // use a TeeWriter for the next section of the report
          val tw = new TeeWriter(qualityWriter, cbcsWriter)
          tw.println(
            s"Barcode\tCondition\tMatched (Construct+Sample Barcode)\tMatched Sample Barcode\t% Match\tNormalized Match"
          )
          colReference.allBarcodes.foreach { colBarcode =>
            val data = perBarcodeQualityData(state, rowReference, colReference, colBarcode)
            tw.println(data.mkString("\t"))
          }

          qualityWriter.println()
          qualityWriter.println("Read counts for most common sample barcodes without associated conditions:")
          val unepectedBarcodeFrequencies =
            state.unknownCol.keys.map(barcode => BarcodeFrequency(barcode, state.unknownCol.count(barcode))).toSeq
          topN(unepectedBarcodeFrequencies, 100).foreach { case BarcodeFrequency(barcode, count) =>
            qualityWriter.println(barcode + "\t" + count.toString)
          }
          qualityWriter.println()
      }
    }

  private def decOptFmt(d: Option[Double]): String = d.map(Decimal00Format.format).getOrElse("N/A")

  private def perBarcodeQualityData[A](
      state: State,
      rowReference: Reference,
      colReference: Reference,
      colBarcode: String
  ): Seq[String] =
    val conditions = colReference.idsForBarcode(colBarcode).mkString(",")
    val matchedRowAndCol: Int =
      rowReference.allBarcodes.map(rowBarcode => state.known.count((rowBarcode, colBarcode))).sum

    val matchedCol = state.knownCol.count(colBarcode)
    val pct = percent(matchedRowAndCol, matchedCol)

    Seq(
      colReference.referenceBarcodeForDnaBarcode(colBarcode),
      conditions,
      matchedRowAndCol.toString,
      matchedCol.toString,
      Decimal00Format.format(pct),
      Decimal000Format.format(logNormalize(matchedRowAndCol, state.reads))
    )

  end perBarcodeQualityData

end QualityWriter
