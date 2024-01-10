/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.nio.file.Files

import scala.io.Source
import scala.util.{Random, Using}

import org.broadinstitute.gpp.poolq3.PoolQ
import org.broadinstitute.gpp.poolq3.barcode.{Barcodes, FoundBarcode}
import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, ReferenceEntry}
import org.broadinstitute.gpp.poolq3.process.ScoringConsumer
import org.broadinstitute.gpp.poolq3.reference.ExactReference
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*

class CorrelationFileTest extends AnyFlatSpec {

  private[this] val Condition1 = "DMSO"
  private[this] val Condition2 = "ITMFA"
  private[this] val Condition3 = "No Drug"
  private[this] val SampleBarcode1 = "GTAT"
  private[this] val SampleBarcode2 = "ACAT"
  private[this] val SampleBarcode3 = "TCAG"

  private[this] val Construct1 = "AACCGGTTAACCGGTTTTAAG"
  private[this] val Construct2 = "CGCTGATTCACGGGATCTAGT"
  private[this] val Construct3 = "TAGTCTGTATCGCCAGCTTCC"
  private[this] val Construct4 = "TGATAGACTAGTGTTGCTGCA"
  private[this] val Constructs = List(Construct1, Construct2, Construct3, Construct4)

  private[this] val rowReference =
    ExactReference(Constructs.map(b => ReferenceEntry(b, b)), identity, includeAmbiguous = false)

  private[this] val colReference =
    ExactReference(
      List(
        ReferenceEntry(SampleBarcode1, Condition1),
        ReferenceEntry(SampleBarcode2, Condition2),
        ReferenceEntry(SampleBarcode3, Condition3)
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
  private[this] val reads =
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
      List.fill(17)((Construct4, SampleBarcode3))

  private[this] val barcodes = CloseableIterable.ofList(Random.shuffle(reads).map { case (row, col) =>
    Barcodes(Some(FoundBarcode(row.toCharArray, 0)), None, Some(FoundBarcode(col.toCharArray, 0)), None)
  })

  "CorrelationFileWriter" should "write a correct correlation file" in {
    val outputFile = Files.createTempFile("correlation-file-test", ".txt")
    try {
      val consumer = new ScoringConsumer(rowReference, colReference, countAmbiguous = true, false, None, None, false)

      val ret = PoolQ.runProcess(barcodes, consumer)
      val state = ret.get.state

      val normalizedCounts = LogNormalizedCountsWriter.logNormalizedCounts(state.known, rowReference, colReference)
      val _ = CorrelationFileWriter.write(outputFile, normalizedCounts, rowReference, colReference)

      val expected =
        s"""\t$Condition1\t$Condition2\t$Condition3
           |$Condition1\t1.00\t0.91\t-0.28
           |$Condition2\t0.91\t1.00\t-0.65
           |$Condition3\t-0.28\t-0.65\t1.00
           |""".stripMargin

      Using.resource(Source.fromFile(outputFile.toFile)) { contents =>
        // now check the contents
        contents.mkString should be(expected)
      }
    } finally {
      val _ = Files.deleteIfExists(outputFile)
    }
  }

  it should "not write a correlation file for a run with a single condition" in {
    val singleCondRef =
      ExactReference(
        List(ReferenceEntry(SampleBarcode1, Condition1), ReferenceEntry(SampleBarcode2, Condition1)),
        identity,
        includeAmbiguous = false
      )
    val outputFile = Files.createTempFile("correlation-file-test", "txt")
    try {
      val consumer = new ScoringConsumer(rowReference, singleCondRef, countAmbiguous = true, false, None, None, false)

      val ret = PoolQ.runProcess(barcodes, consumer)
      val state = ret.get.state

      val normalizedCounts = LogNormalizedCountsWriter.logNormalizedCounts(state.known, rowReference, singleCondRef)

      // we've set a trap - if we try to compute a correlation, the library code should throw an exception
      val _ = noException should be thrownBy {
        CorrelationFileWriter.write(outputFile, normalizedCounts, rowReference, singleCondRef)
      }

      // be sure also that we didn't actually write anything to the file
      Using(Source.fromFile(outputFile.toFile))(src => src.getLines().mkString("\n") should be(""))

    } finally {
      val _ = Files.deleteIfExists(outputFile)
    }
  }

  it should "not write a correlation file for a run with a single row barcode" in {
    val rowReference =
      ExactReference(Constructs.take(1).map(b => ReferenceEntry(b, b)), identity, includeAmbiguous = false)
    val singleCondRef =
      ExactReference(
        List(ReferenceEntry(SampleBarcode1, Condition1), ReferenceEntry(SampleBarcode2, Condition2)),
        identity,
        includeAmbiguous = false
      )
    val outputFile = Files.createTempFile("correlation-file-test", "txt")
    try {
      val consumer = new ScoringConsumer(rowReference, singleCondRef, countAmbiguous = true, false, None, None, false)

      val ret = PoolQ.runProcess(barcodes, consumer)
      val state = ret.get.state

      val normalizedCounts = LogNormalizedCountsWriter.logNormalizedCounts(state.known, rowReference, singleCondRef)

      // we've set a trap - if we try to compute a correlation, the library code should throw an exception
      val _ = noException should be thrownBy {
        CorrelationFileWriter.write(outputFile, normalizedCounts, rowReference, singleCondRef)
      }

      // be sure also that we didn't actually write anything to the file
      Using(Source.fromFile(outputFile.toFile))(src => src.getLines().mkString("\n") should be(""))

    } finally {
      val _ = Files.deleteIfExists(outputFile)
    }
  }

}
