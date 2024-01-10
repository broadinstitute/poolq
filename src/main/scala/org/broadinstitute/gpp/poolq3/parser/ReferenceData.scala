/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.io.{BufferedReader, FileInputStream, InputStreamReader}
import java.nio.file.Path

import scala.util.Using

import com.github.tototoshi.csv.*
import org.apache.commons.io.ByteOrderMark
import org.apache.commons.io.input.BOMInputStream
import org.broadinstitute.gpp.poolq3.reports.{GctDialect, PoolQ2Dialect, ReportsDialect}
import org.broadinstitute.gpp.poolq3.seq.isReferenceBarcode
import org.log4s.{Logger, getLogger}

class ReferenceData(val mappings: Seq[ReferenceEntry]) {
  require(mappings.nonEmpty, "Reference data may not be empty")
  ReferenceData.checkLengths(mappings)

  def barcodeLength: Int = mappings.head.barcodeLength

  def barcodeLengths: (Int, Int) = mappings.head.barcodeLengths

  def forColumnBarcodes(dialect: ReportsDialect): ReferenceData = {
    val columnBarcodeMappings = mappings.map { m =>
      if m.referenceId.isEmpty then m.copy(referenceId = ReferenceData.unlabeled(dialect)) else m
    }
    new ReferenceData(columnBarcodeMappings)
  }

}

object ReferenceData {

  private[this] val log: Logger = getLogger

  val UnlabeledSampleBarcodes = "Unlabeled Sample Barcodes"
  val UnlabeledColumnBarcodes = "Unlabeled Column Barcodes"

  def unlabeled(dialect: ReportsDialect): String = dialect match {
    case PoolQ2Dialect | GctDialect => UnlabeledSampleBarcodes
    case _                          => UnlabeledColumnBarcodes
  }

  // this is complicated because it handles the case where the DNA barcode is quoted
  // matches `"[ACGTacgt:;-]+ *"` or `[ACGTacgt:;-]+ *`,
  // followed by either a tab or a comma
  private[parser] val LineRegex = """^(?:(?:"[ACGTacgt:;-]+ *")|(?:[ACGTacgt:;-]+ *))([\t,]).*$""".r

  private[this] val DelimiterRegex = """^(?:[^,\t]+)([\t,]).+$""".r

  private[parser] def guessDelimiter(br: BufferedReader): Char = {
    br.mark(1024)
    val iter = br.lines().iterator()
    val ret =
      if iter.hasNext then {
        iter.next() match {
          case DelimiterRegex(d) => d.head
          case _                 => ','
        }
      } else ','
    br.reset()
    ret
  }

  def apply(file: Path, quote: Char = '"'): ReferenceData = {
    Using.resource(new FileInputStream(file.toFile)) { fin =>
      val in = BOMInputStream
        .builder()
        .setInputStream(fin)
        .setByteOrderMarks(ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE)
        .setInclude(false)
        .get()
      val br = new BufferedReader(new InputStreamReader(in))
      val guessedDelimiter = guessDelimiter(br)
      implicit object CSVFormat extends DefaultCSVFormat {
        override val delimiter = guessedDelimiter
        override val quoteChar: Char = quote
      }
      skipHeader(br, LineRegex)
      val rows = CSVReader.open(br).all()
      val barcodes = rows.map { case xs =>
        xs match {
          case barcodeRaw :: idRaw :: _ =>
            // if the CSV parser leaves spaces, we should remove them
            val barcode = barcodeRaw.trim()
            val id = idRaw.trim()

            // N.B. empty IDs are commonly used and must be supported; as long as the barcode is a non-empty, valid
            // DNA string, we must accept the row. However, sometimes Excel leaves empty lines in exported CSV; as
            // long as *both* the barcode and ID are empty, it's safe to just skip the row. For now we'll be paranoid
            // and reject cases where the barcode is empty but the ID is non-empty
            if barcode.isEmpty && id.isEmpty then None
            else if isReferenceBarcode(barcode) then Some(ReferenceEntry(barcode, id))
            else throw InvalidFileException(file, s"Invalid DNA barcode '$barcode' for ID '$id'")
          case _ =>
            throw InvalidFileException(
              file,
              s"Incorrect number of columns. At least 2 required, got: ${xs.length}: $xs"
            )
        }
      }

      if barcodes.isEmpty then {
        throw InvalidFileException(file, "Empty reference file")
      }

      new ReferenceData(barcodes.flatten)
    }
  }

  private[parser] def checkLengths(mappings: Seq[ReferenceEntry]): Unit = {
    val barcodesByLength = mappings.groupBy(_.barcodeLength)
    if barcodesByLength.keySet.size == 1 then ()
    else {
      // grab the first thing in each size grouping
      val examples = barcodesByLength.toSeq.flatMap { case (length, barcodes) =>
        barcodes.headOption.map(bc => length -> bc)
      }

      // sort them by size and get a text description
      val sortedExamples =
        examples
          .sortBy { case (length, _) => length }
          .map { case (length, example) => s"${example.referenceBarcode} is $length bases" }

      // log the problem and throw
      log.error(s"Examples: ${sortedExamples.mkString(", ")}")
      throw new IllegalArgumentException(s"Input barcodes must all be of the same length")
    }
  }

  def truncator(newLength: Int): String => String = s => s.substring(0, newLength)

}
