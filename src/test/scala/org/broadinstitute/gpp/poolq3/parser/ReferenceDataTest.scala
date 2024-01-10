/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.io.{BufferedReader, StringReader}

import org.broadinstitute.gpp.poolq3.TestResources
import org.broadinstitute.gpp.poolq3.reports.PoolQ2Dialect
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*

class ReferenceDataTest extends AnyFlatSpec with TestResources {

  "truncateMapping" should "truncate the barcode in a mapping" in {
    val actual = ReferenceData.truncator(15)("AAAAACCCCCGGGGGTTTTT")
    val expected = "AAAAACCCCCGGGGG"
    actual should be(expected)
  }

  "truncateMapping" should "not change the length if the original length is given" in {
    val bc = "AAAAACCCCCGGGGGTTTTT"
    val actual = ReferenceData.truncator(bc.length)(bc)
    val expected = bc
    actual should be(expected)
  }

  "guessDelimiter" should "guess the correct delimiter" in {
    val source =
      s"""THIS IS A COLUMN\tTHisIS Another Column
         |AACGTGTA;TCCGTGATG\tThis is a condition
         |""".stripMargin
    val r = new BufferedReader(new StringReader(source))
    ReferenceData.guessDelimiter(r) should be('\t')
  }

  "ReferenceData" should "read a csv file with no header" in {
    val path = resourcePath("reference.csv")
    ReferenceData(path).mappings should be(
      Seq(
        ReferenceEntry("AAAAAAAAAA", "This is a simple barcode"),
        ReferenceEntry("AAAA;AAAAAT", "This barcode has a semicolon")
      )
    )
  }

  it should "read a csv with a header" in {
    val path = resourcePath("reference_header.csv")
    ReferenceData(path).mappings should be(
      Seq(
        ReferenceEntry("AAAAAAAAAA", "This is a simple barcode"),
        ReferenceEntry("AAAA;AAAAAT", "This barcode has a semicolon")
      )
    )
  }

  it should "read a csv with empty fields in some rows" in {
    val path = resourcePath("reference_header_empty_rows.csv")
    ReferenceData(path).mappings should be(
      Seq(
        ReferenceEntry("AAAAAAAAAA", "This is a simple barcode"),
        ReferenceEntry("AAAA;AAAAAT", "This barcode has a semicolon")
      )
    )
  }

  it should "read a tsv file with no header" in {
    val path = resourcePath("reference.txt")
    ReferenceData(path).mappings should be(
      Seq(
        ReferenceEntry("AAAAAAAAAA", "This is a simple barcode"),
        ReferenceEntry("AAAA;AAAAAT", "This barcode has a semicolon")
      )
    )
  }

  it should "read a tsv file with a header" in {
    val path = resourcePath("reference_header.txt")
    ReferenceData(path).mappings should be(
      Seq(
        ReferenceEntry("AAAAAAAAAA", "This is a simple barcode"),
        ReferenceEntry("AAAA;AAAAAT", "This barcode has a semicolon")
      )
    )
  }

  it should "read a CSV with empty IDs" in {
    val path = resourcePath("reference_empty_id.csv")
    val rd1 = ReferenceData(path)
    val _ = rd1.mappings should be(Seq(ReferenceEntry("TTGAACCG", "EMPTY"), ReferenceEntry("GGCTTGCG", "")))
    val rd2 = rd1.forColumnBarcodes(PoolQ2Dialect)
    rd2.mappings should be(
      Seq(ReferenceEntry("TTGAACCG", "EMPTY"), ReferenceEntry("GGCTTGCG", "Unlabeled Sample Barcodes"))
    )
  }

  it should "reject a CSV with empty barcodes but non-empty ID" in {
    val path = resourcePath("reference_empty_barcode.csv")
    val e = intercept[InvalidFileException](ReferenceData(path))
    e.msg.exists(_.contains("Here's an ID with no barcode!")) should be(true)
  }

}
