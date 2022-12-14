/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.nio.file.Files

import scala.io.Source
import scala.util.{Random, Using}

import better.files._
import org.broadinstitute.gpp.poolq3.PoolQ
import org.broadinstitute.gpp.poolq3.barcode.{Barcodes, FoundBarcode}
import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, ReferenceEntry}
import org.broadinstitute.gpp.poolq3.process.ScoringConsumer
import org.broadinstitute.gpp.poolq3.reference.{ExactReference, VariantReference}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class UnexpectedSequencesTest extends AnyFlatSpec {

  private[this] val rowReferenceBarcodes =
    List("AAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAC", "AAAAAAAAAAAAAAAAAAAG", "AAAAAAAAAAAAAAAAAAAT").map(b =>
      ReferenceEntry(b, b)
    )

  private[this] val colReferenceBarcodes = List(
    ReferenceEntry("AAAA", "Eh"),
    ReferenceEntry("AAAT", "Eh"),
    ReferenceEntry("CCCC", "Sea"),
    ReferenceEntry("CCCG", "Sea")
  )

  private[this] val rowReference = VariantReference(rowReferenceBarcodes, identity, includeAmbiguous = false)
  private[this] val colReference = ExactReference(colReferenceBarcodes, identity, includeAmbiguous = false)

  private[this] val globalReference =
    ExactReference(List(ReferenceEntry("GATGTGCAGTGAGTAGCGAG", "Oh, that one")), identity, includeAmbiguous = false)

  private[this] val unexpectedReadCount = Random.nextInt(200)
  private[this] val expectedReadCount = Random.nextInt(1000)

  // these reads should not be included in the unexpected sequence report for reasons noted below
  private[this] val expectedReads =
    List(
      ("AAAAAAAAAAAAAAAAAAAA", "TTTT"), // known row barcode, unknown column barcode
      ("AAAAAAAAAAAAAAAAAAAC", "AAAA"), // known row barcode, known column barcode
      ("TTTTTTTTTTTTTTTTTTTT", "TTTT"), // unknown row barcode, unknown column barcode
      ("NATGTGCAGTGAGTAGCGAG", "CCCC") // unknown row barcode with an N, known column barcode
    )

  // these reads should be included in the unexpected sequence report for reasons noted below
  private[this] val unexpectedReads =
    List(
      ("TTTTTTTTTTTTTTTTTTTT", "AAAA"), // unknown row barcode, known column barcode
      ("GATGTGCAGTGAGTAGCGAG", "CCCG") // unknown row barcode, known column barcode
    )

  private[this] val underlyingBarcodes =
    Random.shuffle(
      List.fill(expectedReadCount)(expectedReads).flatten ++ List.fill(unexpectedReadCount)(unexpectedReads).flatten
    )

  private[this] val barcodes = CloseableIterable.ofList(underlyingBarcodes.map { case (row, col) =>
    Barcodes(Some(FoundBarcode(row.toCharArray, 0)), None, Some(FoundBarcode(col.toCharArray, 0)), None)
  })

  "PoolQ" should "report unexpected sequences" in {
    val tmpPath = Files.createTempDirectory("unexpected-sequences-test")
    try {
      val outputFile = tmpPath.resolve("unexpected-sequences.txt")
      val cachePath = tmpPath.resolve("cache")
      val consumer =
        new ScoringConsumer(rowReference, colReference, countAmbiguous = true, false, None, Some(cachePath), false)

      // run PoolQ and write the file
      PoolQ.runProcess(barcodes, consumer)

      UnexpectedSequenceWriter.write(outputFile, cachePath, 100, colReference, Some(globalReference))

      val expected =
        s"""Sequence\tTotal\tAAAA\tAAAT\tCCCC\tCCCG\tPotential IDs
           |GATGTGCAGTGAGTAGCGAG\t$unexpectedReadCount\t0\t0\t0\t$unexpectedReadCount\tOh, that one
           |TTTTTTTTTTTTTTTTTTTT\t$unexpectedReadCount\t$unexpectedReadCount\t0\t0\t0\t
           |""".stripMargin

      Using.resource(Source.fromFile(outputFile.toFile)) { contents =>
        // now check the contents
        contents.mkString should be(expected)
      }

    } finally {
      val _ = tmpPath.toFile.toScala.delete()
    }

  }

}
