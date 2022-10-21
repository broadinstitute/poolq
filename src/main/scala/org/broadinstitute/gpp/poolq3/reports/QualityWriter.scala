/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
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

object QualityWriter {

  def write(
    file: Path,
    state: State,
    rowReference: Reference,
    colReference: Reference,
    isPairedEnd: Boolean
  ): Try[Unit] =
    Using(new PrintWriter(file.toFile)) { writer =>
      val barcodeLocationStats =
        if (isPairedEnd) {
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

        } else {
          s"""Reads with no construct barcode: ${state.rowBarcodeNotFound}
             |Max construct barcode index: ${state.rowBarcodeStats.maxPosStr}
             |Min construct barcode index: ${state.rowBarcodeStats.minPosStr}
             |Avg construct barcode index: ${decOptFmt(state.rowBarcodeStats.avg)}""".stripMargin
        }

      val header =
        s"""Total reads: ${state.reads}
           |Matching reads: ${state.matches}
           |1-base mismatch reads: ${state.matches - state.exactMatches}
           |
           |Overall % match: ${Decimal00Format.format(state.matchPercent)}
           |
           |$barcodeLocationStats
           |""".stripMargin

      writer.println(header)

      writer.println(s"Read counts for sample barcodes with associated conditions:")
      writer.println(
        s"Barcode\tCondition\tMatched (Construct+Sample Barcode)\tMatched Sample Barcode\t% Match\tNormalized Match"
      )
      colReference.allBarcodes.foreach { colBarcode =>
        val data = perBarcodeQualityData(state, rowReference, colReference, colBarcode)
        writer.println(data.mkString("\t"))
      }

      writer.println()
      writer.println("Read counts for most common sample barcodes without associated conditions:")
      val unepectedBarcodeFrequencies =
        state.unknownCol.keys.map(barcode => BarcodeFrequency(barcode, state.unknownCol.count(barcode))).toSeq
      topN(unepectedBarcodeFrequencies, 100).foreach { case BarcodeFrequency(barcode, count) =>
        writer.println(barcode + "\t" + count.toString)
      }
      writer.println()
    }

  private[this] def decOptFmt(d: Option[Double]): String = d.map(Decimal00Format.format).getOrElse("N/A")

  private[this] def perBarcodeQualityData[A](
    state: State,
    rowReference: Reference,
    colReference: Reference,
    colBarcode: String
  ): Seq[String] = {
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
  }

}
