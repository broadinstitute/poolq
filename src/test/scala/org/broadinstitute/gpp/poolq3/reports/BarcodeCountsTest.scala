/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.nio.file.{Files, Path}
import java.util.function.Predicate
import java.util.stream.Collectors

import cats.syntax.all.*
import munit.FunSuite
import org.broadinstitute.gpp.poolq3.PoolQ
import org.broadinstitute.gpp.poolq3.barcode.{Barcodes, FoundBarcode}
import org.broadinstitute.gpp.poolq3.hist.{BasicShardedHistogram, OpenHashMapHistogram, TupleHistogram}
import org.broadinstitute.gpp.poolq3.parser.{BarcodeSet, CloseableIterable, ReferenceEntry}
import org.broadinstitute.gpp.poolq3.process.{ScoringConsumer, State}
import org.broadinstitute.gpp.poolq3.reference.ExactReference
import org.broadinstitute.gpp.poolq3.testutil.contents

class BarcodeCountsTest extends FunSuite:

  private val Condition1 = "DMSO"
  private val Condition2 = "ITMFA"
  private val Condition3 = "No Drug"
  private val SampleBarcode1 = "GTAT"
  private val SampleBarcode2 = "ACAT"
  private val SampleBarcode3 = "TCAG"
  private val SampleBarcode4 = "TCCG"

  private val Construct1 = "AACCGGTTAACCGGTTTTAAG"
  private val Construct2 = "CGCTGATTCACGGGATCTAGT"
  private val Construct3 = "TAGTCTGTATCGCCAGCTTCC"
  private val Construct4 = "TGATAGACTAGTGTTGCTGCA"
  private val ConstructId1 = "BRDN01"
  private val ConstructId2 = "BRDN02"
  private val ConstructId3 = "BRDN03"
  private val ConstructId4 = "BRDN04"
  private val ConstructId5 = "BRDN04"

  private val Constructs =
    List(
      ReferenceEntry(Construct1, ConstructId1),
      ReferenceEntry(Construct1, ConstructId5),
      ReferenceEntry(Construct2, ConstructId2),
      ReferenceEntry(Construct3, ConstructId3),
      ReferenceEntry(Construct4, ConstructId4)
    )

  private val rowReference = ExactReference(Constructs, identity, includeAmbiguous = false)

  private val colReference =
    ExactReference(
      List(
        ReferenceEntry(SampleBarcode1, Condition1),
        ReferenceEntry(SampleBarcode2, Condition2),
        ReferenceEntry(SampleBarcode3, Condition3),
        ReferenceEntry(SampleBarcode4, Condition3)
      ),
      identity,
      includeAmbiguous = false
    )

  // these reads correspond to the following counts matrix
  //      c1     c2     c3
  // b1   13     1      97
  // b2   23     26     6
  // b3   66     73     32
  // b4   45     68     17
  private val reads =
    List.fill(13)((Construct1, SampleBarcode1)) ++
      List.fill(1)((Construct1, SampleBarcode2)) ++
      List.fill(97)((Construct1, SampleBarcode3)) ++
      List.fill(23)((Construct2, SampleBarcode1)) ++
      List.fill(26)((Construct2, SampleBarcode2)) ++
      List.fill(6)((Construct2, SampleBarcode3)) ++
      List.fill(66)((Construct3, SampleBarcode1)) ++
      List.fill(73)((Construct3, SampleBarcode2)) ++
      List.fill(32)((Construct3, SampleBarcode3)) ++
      List.fill(45)((Construct4, SampleBarcode1)) ++
      List.fill(68)((Construct4, SampleBarcode2)) ++
      List.fill(17)((Construct4, SampleBarcode4))

  private val barcodes = CloseableIterable.ofList(reads.map { case (row, col) =>
    Barcodes(Some(FoundBarcode(row.toCharArray, 0)), None, Some(FoundBarcode(col.toCharArray, 0)), None)
  })

  test("BarcodeCountsWriter should write a correct counts file") {
    val outputFile = Files.createTempFile("barcode-counts-file-test", ".txt")
    try
      val consumer = new ScoringConsumer(rowReference, colReference, countAmbiguous = true, false, None, None, false)

      val ret = PoolQ.runProcess(barcodes, consumer)
      val state = ret.get.state

      val _ = BarcodeCountsWriter.write(outputFile, state.known, rowReference, colReference, PoolQ2Dialect)

      val expected =
        s"""Construct Barcode\tConstruct IDs\t$SampleBarcode1\t$SampleBarcode2\t$SampleBarcode3\t$SampleBarcode4
           |$Construct1\t$ConstructId1,$ConstructId5\t13\t1\t97\t0
           |$Construct2\t$ConstructId2\t23\t26\t6\t0
           |$Construct3\t$ConstructId3\t66\t73\t32\t0
           |$Construct4\t$ConstructId4\t45\t68\t0\t17
           |""".stripMargin

      assertEquals(contents(outputFile), expected)
    finally
      val _ = Files.deleteIfExists(outputFile)
    end try
  }

  test("BarcodeCountsWriter should write a GCT file") {
    val outputFile = Files.createTempFile("barcode-counts-file-test", ".gct")
    try
      val consumer = new ScoringConsumer(rowReference, colReference, countAmbiguous = true, false, None, None, false)

      val ret = PoolQ.runProcess(barcodes, consumer)
      val state = ret.get.state

      val _ = BarcodeCountsWriter.write(outputFile, state.known, rowReference, colReference, GctDialect)

      val expected =
        s"""#1.2
           |4\t4
           |NAME\tDescription\t$SampleBarcode1\t$SampleBarcode2\t$SampleBarcode3\t$SampleBarcode4
           |$Construct1\t$ConstructId1,$ConstructId5\t13\t1\t97\t0
           |$Construct2\t$ConstructId2\t23\t26\t6\t0
           |$Construct3\t$ConstructId3\t66\t73\t32\t0
           |$Construct4\t$ConstructId4\t45\t68\t0\t17
           |""".stripMargin

      assertEquals(contents(outputFile), expected)
    finally
      val _ = Files.deleteIfExists(outputFile)
    end try
  }

  test("CountsWriter should write UMI files") {
    // row barcodes
    val brdn01 = "AAAAAAAAAAAAAAAAAAAA"
    val brdn02 = "GATGTGCAGTGAGTAGCGAG"
    val brdn03 = "CCGGTTGATGCGTGGTGATG"
    val rowReferenceBarcodes =
      List(brdn01, brdn02, brdn03).map(b => ReferenceEntry(b, b))

    // column barcodes
    val eh1 = "AAAA"
    val eh2 = "AAAT"
    val sea1 = "CCCC"
    val sea2 = "CCCG"
    val colReferenceBarcodes =
      List(
        ReferenceEntry(eh1, "Eh"),
        ReferenceEntry(eh2, "Eh"),
        ReferenceEntry(sea1, "Sea"),
        ReferenceEntry(sea2, "Sea")
      )

    // umi barcodes
    val a01 = "GAAAA"
    val a03 = "GCCCC"
    val e09 = "GTTTT"
    val f02 = "AGGGG"
    val umiBarcodes = new BarcodeSet(Set(a01, a03, e09, f02))

    val rowReference = ExactReference(rowReferenceBarcodes, identity, includeAmbiguous = true)
    val colReference = ExactReference(colReferenceBarcodes, identity, includeAmbiguous = false)

    val state = new State(
      new BasicShardedHistogram(new TupleHistogram),
      new OpenHashMapHistogram,
      new OpenHashMapHistogram,
      new OpenHashMapHistogram
    )

    val _ = state.known.increment(None, (brdn01, eh2))
    val _ = state.known.increment(a01.some, (brdn01, eh1))
    val _ = state.known.increment(a01.some, (brdn01, eh1))
    val _ = state.known.increment(a01.some, (brdn01, eh2))
    val _ = state.known.increment(e09.some, (brdn01, eh1))

    val _ = state.known.increment(a03.some, (brdn02, eh1))
    val _ = state.known.increment(a03.some, (brdn02, sea2))

    val _ = state.known.increment(f02.some, (brdn03, sea1))
    val _ = state.known.increment(a03.some, (brdn03, sea1))

    val aggregateOutputFile = Files.createTempFile("barcode-counts-file-test-umi-", ".txt")

    val outputFileFilter: Predicate[Path] = f =>
      f.getFileName.toString.startsWith("barcode-counts-file-test-umi-") &&
        f.getFileName.toString.endsWith(".txt") &&
        f.getFileName != aggregateOutputFile.getFileName

    val bcre = "barcode-counts-file-test-umi-.+-([ACGT]{5}|UNMATCHED-UMI).txt".r
    def umiBarcodeFor(f: Path): Option[String] = f.getFileName.toString match
      case bcre(umi) => umi.some
      case _ => None

    try
      val _ = BarcodeCountsWriter.write(
        aggregateOutputFile,
        aggregateOutputFile.getParent.some,
        state.known,
        rowReference,
        colReference,
        umiBarcodes.some,
        PoolQ3Dialect
      )

      val expectedAll =
        s"""Row Barcode\tRow Barcode IDs\tAAAA\tAAAT\tCCCC\tCCCG
           |$brdn01\t$brdn01\t3\t2\t0\t0
           |$brdn02\t$brdn02\t1\t0\t0\t1
           |$brdn03\t$brdn03\t0\t0\t2\t0
           |""".stripMargin

      assertEquals(contents(aggregateOutputFile), expectedAll)

      val umiFiles = Files.list(aggregateOutputFile.getParent).filter(outputFileFilter).collect(Collectors.toList[Path])

      var umiFileQualifiers: List[String] = Nil
      umiFiles.forEach { umiFile =>
        val qualifier = umiBarcodeFor(umiFile)
        qualifier.foreach(q => umiFileQualifiers ::= q)
        qualifier match
          case None => fail(s"no UMI barcode found for $umiFile")
          case Some("UNMATCHED-UMI") =>
            val expected =
              s"""Row Barcode\tRow Barcode IDs\tAAAA\tAAAT\tCCCC\tCCCG
                 |$brdn01\t$brdn01\t0\t1\t0\t0
                 |$brdn02\t$brdn02\t0\t0\t0\t0
                 |$brdn03\t$brdn03\t0\t0\t0\t0
                 |""".stripMargin
            assertEquals(contents(umiFile), expected)
          case Some(`a01`) =>
            val expected =
              s"""Row Barcode\tRow Barcode IDs\tAAAA\tAAAT\tCCCC\tCCCG
                 |$brdn01\t$brdn01\t2\t1\t0\t0
                 |$brdn02\t$brdn02\t0\t0\t0\t0
                 |$brdn03\t$brdn03\t0\t0\t0\t0
                 |""".stripMargin
            assertEquals(contents(umiFile), expected)
          case Some(`a03`) =>
            val expected =
              s"""Row Barcode\tRow Barcode IDs\tAAAA\tAAAT\tCCCC\tCCCG
                 |$brdn01\t$brdn01\t0\t0\t0\t0
                 |$brdn02\t$brdn02\t1\t0\t0\t1
                 |$brdn03\t$brdn03\t0\t0\t1\t0
                 |""".stripMargin
            assertEquals(contents(umiFile), expected)
          case Some(`e09`) =>
            val expected =
              s"""Row Barcode\tRow Barcode IDs\tAAAA\tAAAT\tCCCC\tCCCG
                 |$brdn01\t$brdn01\t1\t0\t0\t0
                 |$brdn02\t$brdn02\t0\t0\t0\t0
                 |$brdn03\t$brdn03\t0\t0\t0\t0
                 |""".stripMargin
            assertEquals(contents(umiFile), expected)
          case Some(`f02`) =>
            val expected =
              s"""Row Barcode\tRow Barcode IDs\tAAAA\tAAAT\tCCCC\tCCCG
                 |$brdn01\t$brdn01\t0\t0\t0\t0
                 |$brdn02\t$brdn02\t0\t0\t0\t0
                 |$brdn03\t$brdn03\t0\t0\t1\t0
                 |""".stripMargin
            assertEquals(contents(umiFile), expected)
          case Some(bc) =>
            fail(s"Unexpected UMI barcode $bc")
        end match
      }
      // make sure we got everything
      assertEquals(umiFileQualifiers.toSet, umiBarcodes.barcodes + "UNMATCHED-UMI")
    finally
      // delete the named output file
      val _ = Files.deleteIfExists(aggregateOutputFile)

      // delete the other associated files
      Files.list(aggregateOutputFile.getParent).filter(outputFileFilter).forEach { f =>
        val _ = Files.deleteIfExists(f)
      }
    end try
  }

end BarcodeCountsTest
