/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.nio.file.Files

import scala.io.Source
import scala.util.{Random, Using}

import better.files.*
import munit.{FunSuite, Location}
import org.broadinstitute.gpp.poolq3.barcode.{Barcodes, FoundBarcode}
import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, ReferenceEntry}
import org.broadinstitute.gpp.poolq3.process.{ScoringConsumer, UnexpectedSequenceTracker}
import org.broadinstitute.gpp.poolq3.reference.{ExactReference, VariantReference}
import org.broadinstitute.gpp.poolq3.{PoolQ, TestResources}

class UnexpectedSequencesTest extends FunSuite with TestResources:

  private val rowReferenceBarcodes =
    List("AAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAC", "AAAAAAAAAAAAAAAAAAAG", "AAAAAAAAAAAAAAAAAAAT").map(b =>
      ReferenceEntry(b, b)
    )

  private val colReferenceBarcodes = List(
    ReferenceEntry("AAAA", "Eh"),
    ReferenceEntry("AAAT", "Eh"),
    ReferenceEntry("CCCC", "Sea"),
    ReferenceEntry("CCCG", "Sea")
  )

  private val rowReference = VariantReference(rowReferenceBarcodes, identity, includeAmbiguous = false)
  private val colReference = ExactReference(colReferenceBarcodes, identity, includeAmbiguous = false)

  private val globalReference =
    ExactReference(List(ReferenceEntry("GATGTGCAGTGAGTAGCGAG", "Oh, that one")), identity, includeAmbiguous = false)

  // these reads should not be included in the unexpected sequence report for reasons noted below
  private val expectedReads =
    List(
      ("AAAAAAAAAAAAAAAAAAAA", "TTTT"), // known row barcode, unknown column barcode
      ("AAAAAAAAAAAAAAAAAAAC", "AAAA"), // known row barcode, known column barcode
      ("TTTTTTTTTTTTTTTTTTTT", "TTTT"), // unknown row barcode, unknown column barcode
      ("NATGTGCAGTGAGTAGCGAG", "CCCC") // unknown row barcode with an N, known column barcode
    )

  // these reads should be included in the unexpected sequence report for reasons noted below
  private val unexpectedReads =
    List(
      ("TTTTTTTTTTTTTTTTTTTT", "AAAA"), // unknown row barcode, known column barcode
      ("GATGTGCAGTGAGTAGCGAG", "CCCG") // unknown row barcode, known column barcode
    )

  test("PoolQ should report unexpected sequences") {
    val unexpectedReadCount = Random.nextInt(200)
    val expectedReadCount = Random.nextInt(1000)

    val underlyingBarcodes =
      Random.shuffle(
        List.fill(expectedReadCount)(expectedReads).flatten ++ List.fill(unexpectedReadCount)(unexpectedReads).flatten
      )

    testIt(underlyingBarcodes, unexpectedReadCount, 1000)
  }

  test("PoolQ should report unexpected sequences found in its sample only") {
    val unexpectedReadCount = Random.nextInt(200)
    val expectedReadCount = Random.nextInt(1000)

    val missedUnexpectedReadCount = Random.nextInt(100)

    val missedUnexpectedReads = List(("CCCCCCCCAAAAAAAAAAAA", "AAAA"), ("GGGGGGGGGGTTTTTTTTTT", "CCCG"))

    val underlyingBarcodes =
      List.concat(
        Random.shuffle(
          List.concat(
            List.fill(expectedReadCount)(expectedReads).flatten,
            List.fill(unexpectedReadCount)(unexpectedReads).flatten
          )
        ),
        // append a number of additional unexpected reads; these won't occur until late in processing,
        // and thus won't be found in the report
        List.fill(missedUnexpectedReadCount)(missedUnexpectedReads).flatten
      )

    testIt(underlyingBarcodes, unexpectedReadCount, 2)

  }

  test("read unexpected sequence cache") {
    val cachePath = resourcePath("unexpected-sequences")
    val outputFile = Files.createTempFile("unexpected", ".txt")
    try
      val unexpectedReadCount = 9

      UnexpectedSequenceWriter
        .write(outputFile, cachePath, 100, colReference, Some(globalReference), 2)
        .get

      val expected =
        s"""Sequence\tTotal\tAAAA\tAAAT\tCCCC\tCCCG\tPotential IDs
           |GATGTGCAGTGAGTAGCGAG\t$unexpectedReadCount\t0\t0\t0\t$unexpectedReadCount\tOh, that one
           |TTTTTTTTTTTTTTTTTTTT\t$unexpectedReadCount\t$unexpectedReadCount\t0\t0\t0\t
           |""".stripMargin

      Using.resource(Source.fromFile(outputFile.toFile)) { contents =>
        // now check the contents
        val actual = contents.mkString
        assertEquals(actual, expected)
      }
    finally
      val _ = Files.deleteIfExists(outputFile)
    end try
  }

  test("breadth-first iterator") {
    import scala.collection.mutable

    class TestCachedBarcodes(val colBc: String, iter: Iterator[String]) extends UnexpectedSequenceWriter.CachedBarcodes:
      var closed = false
      override def hasNext: Boolean = iter.hasNext
      override def next(): String = iter.next()
      override def close(): Unit = closed = true

    val i1 = new TestCachedBarcodes("AAA", Iterator("AAAAAA"))
    val i2 = new TestCachedBarcodes("CCC", Iterator("AAAAAA", "AAAAAT", "AAAAAA"))
    val i3 = new TestCachedBarcodes("GGG", Iterator("AAAAAA"))
    val i4 = new TestCachedBarcodes("TTT", Iterator("AAAAAA"))

    val circularBuffer: UnexpectedSequenceWriter.BreadthFirstIterator =
      val readers = mutable.Queue[UnexpectedSequenceWriter.CachedBarcodes](i1, i2, i3, i4)
      new UnexpectedSequenceWriter.BreadthFirstIterator(readers)

    val barcodes = circularBuffer.toList
    assertEquals(
      barcodes,
      List(
        ("AAAAAA", "AAA"),
        ("AAAAAA", "CCC"),
        ("AAAAAA", "GGG"),
        ("AAAAAA", "TTT"),
        ("AAAAAT", "CCC"),
        ("AAAAAA", "CCC")
      )
    )
    assert(i1.closed)
    assert(i2.closed)
    assert(i3.closed)
    assert(i4.closed)
  }

  private def testIt(underlyingBarcodes: List[(String, String)], unexpectedReadCount: Int, maxMapSize: Int)(implicit
      loc: Location
  ): Unit =
    val barcodes = CloseableIterable.ofList(underlyingBarcodes.map { case (row, col) =>
      Barcodes(Some(FoundBarcode(row.toCharArray, 0)), None, Some(FoundBarcode(col.toCharArray, 0)), None)
    })
    val tmpPath = Files.createTempDirectory("unexpected-sequences-test")
    try
      val outputFile = tmpPath.resolve("unexpected-sequences.txt")
      val cachePath = tmpPath.resolve("cache")
      val ust = new UnexpectedSequenceTracker(cachePath, colReference)
      val consumer =
        new ScoringConsumer(rowReference, colReference, countAmbiguous = true, false, None, Some(ust), false)

      // run PoolQ and write the file
      val _ = PoolQ.runProcess(barcodes, consumer).get

      UnexpectedSequenceWriter
        .write(outputFile, cachePath, 100, colReference, Some(globalReference), maxMapSize)
        .get

      val expected =
        s"""Sequence\tTotal\tAAAA\tAAAT\tCCCC\tCCCG\tPotential IDs
           |GATGTGCAGTGAGTAGCGAG\t$unexpectedReadCount\t0\t0\t0\t$unexpectedReadCount\tOh, that one
           |TTTTTTTTTTTTTTTTTTTT\t$unexpectedReadCount\t$unexpectedReadCount\t0\t0\t0\t
           |""".stripMargin

      Using.resource(Source.fromFile(outputFile.toFile)) { contents =>
        // now check the contents
        val actual = contents.mkString
        assertEquals(actual, expected)
      }
    finally
      val _ = tmpPath.toFile.toScala.delete()

    end try

  end testIt

end UnexpectedSequencesTest
