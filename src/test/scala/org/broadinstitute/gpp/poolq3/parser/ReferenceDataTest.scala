/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.io.{BufferedReader, StringReader}

import munit.FunSuite
import org.broadinstitute.gpp.poolq3.TestResources
import org.broadinstitute.gpp.poolq3.reports.PoolQ2Dialect

class ReferenceDataTest extends FunSuite with TestResources:

  test("truncateMapping should truncate the barcode in a mapping") {
    val actual = ReferenceData.truncator(15)("AAAAACCCCCGGGGGTTTTT")
    val expected = "AAAAACCCCCGGGGG"
    assertEquals(actual, expected)
  }

  test("truncateMapping should not change the length if the original length is given") {
    val bc = "AAAAACCCCCGGGGGTTTTT"
    val actual = ReferenceData.truncator(bc.length)(bc)
    val expected = bc
    assertEquals(actual, expected)
  }

  test("guessDelimiter should guess the correct delimiter") {
    val source =
      s"""THIS IS A COLUMN\tTHisIS Another Column
         |AACGTGTA;TCCGTGATG\tThis is a condition
         |""".stripMargin
    val r = new BufferedReader(new StringReader(source))
    assertEquals(ReferenceData.guessDelimiter(r), '\t')
  }

  test("ReferenceData should read a csv file with no header") {
    val path = resourcePath("reference.csv")
    assertEquals(
      ReferenceData(path).mappings,
      Seq(
        ReferenceEntry("AAAAAAAAAA", "This is a simple barcode"),
        ReferenceEntry("AAAA;AAAAAT", "This barcode has a semicolon")
      )
    )
  }

  test("should read a csv with a header") {
    val path = resourcePath("reference_header.csv")
    assertEquals(
      ReferenceData(path).mappings,
      Seq(
        ReferenceEntry("AAAAAAAAAA", "This is a simple barcode"),
        ReferenceEntry("AAAA;AAAAAT", "This barcode has a semicolon")
      )
    )
  }

  test("should read a csv with empty fields in some rows") {
    val path = resourcePath("reference_header_empty_rows.csv")
    assertEquals(
      ReferenceData(path).mappings,
      Seq(
        ReferenceEntry("AAAAAAAAAA", "This is a simple barcode"),
        ReferenceEntry("AAAA;AAAAAT", "This barcode has a semicolon")
      )
    )
  }

  test("should read a tsv file with no header") {
    val path = resourcePath("reference.txt")
    assertEquals(
      ReferenceData(path).mappings,
      Seq(
        ReferenceEntry("AAAAAAAAAA", "This is a simple barcode"),
        ReferenceEntry("AAAA;AAAAAT", "This barcode has a semicolon")
      )
    )
  }

  test("should read a tsv file with a header") {
    val path = resourcePath("reference_header.txt")
    assertEquals(
      ReferenceData(path).mappings,
      Seq(
        ReferenceEntry("AAAAAAAAAA", "This is a simple barcode"),
        ReferenceEntry("AAAA;AAAAAT", "This barcode has a semicolon")
      )
    )
  }

  test("should read a CSV with empty IDs") {
    val path = resourcePath("reference_empty_id.csv")
    val rd1 = ReferenceData(path)
    assertEquals(rd1.mappings, Seq(ReferenceEntry("TTGAACCG", "EMPTY"), ReferenceEntry("GGCTTGCG", "")))
    val rd2 = rd1.forColumnBarcodes(PoolQ2Dialect)
    assertEquals(
      rd2.mappings,
      Seq(ReferenceEntry("TTGAACCG", "EMPTY"), ReferenceEntry("GGCTTGCG", "Unlabeled Sample Barcodes"))
    )
  }

  test("should reject a CSV with empty barcodes but non-empty ID") {
    val path = resourcePath("reference_empty_barcode.csv")
    val e = intercept[InvalidFileException](ReferenceData(path))
    assert(e.msg.exists(_.contains("Here's an ID with no barcode!")))
  }

end ReferenceDataTest
