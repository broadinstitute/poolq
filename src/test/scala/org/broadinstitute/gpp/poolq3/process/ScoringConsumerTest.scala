/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.process

import munit.FunSuite
import org.broadinstitute.gpp.poolq3.barcode.{Barcodes, FoundBarcode}
import org.broadinstitute.gpp.poolq3.parser.{BarcodeSet, ReferenceEntry}
import org.broadinstitute.gpp.poolq3.reference.{ExactReference, Reference}

class ScoringConsumerTest extends FunSuite:

  val rowReference: Reference =
    ExactReference(Seq(ReferenceEntry("AAAAAAAAAA", "Barcode1")), identity, includeAmbiguous = false)

  val colReference: Reference =
    ExactReference(Seq(ReferenceEntry("AAA", "Condition1")), identity, includeAmbiguous = false)

  test("found matching row and column") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, None, None, false)
    val barcodes =
      Barcodes(Some(FoundBarcode("AAAAAAAAAA".toCharArray, 23)), None, Some(FoundBarcode("AAA".toCharArray, 0)), None)

    consumer.consume(barcodes)
    val state = consumer.state

    assertEquals(state.known.count(("AAAAAAAAAA", "AAA")), 1L)
    assertEquals(state.knownCol.count("AAA"), 1L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 1L)
    assertEquals(state.matches, 1L)
    assertEquals(state.rowBarcodeNotFound, 0L)
    assertEquals(state.rowBarcodeStats.min, 23)
    assertEquals(state.rowBarcodeStats.max, 23)
  }

  test("found matching rows and columns") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, None, None, false)
    val barcodes1 =
      Barcodes(Some(FoundBarcode("AAAAAAAAAA".toCharArray, 23)), None, Some(FoundBarcode("AAA".toCharArray, 0)), None)
    val barcodes2 =
      Barcodes(Some(FoundBarcode("AAAAAAAAAA".toCharArray, 22)), None, Some(FoundBarcode("AAA".toCharArray, 0)), None)

    consumer.consume(barcodes1)
    consumer.consume(barcodes2)
    val state = consumer.state

    assertEquals(state.known.count(("AAAAAAAAAA", "AAA")), 2L)
    assertEquals(state.knownCol.count("AAA"), 2L)
    assertEquals(state.reads, 2L)
    assertEquals(state.exactMatches, 2L)
    assertEquals(state.matches, 2L)
    assertEquals(state.rowBarcodeNotFound, 0L)
    assertEquals(state.rowBarcodeStats.min, 22)
    assertEquals(state.rowBarcodeStats.max, 23)
  }

  test("found matching column") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, None, None, false)
    val barcodes =
      Barcodes(Some(FoundBarcode("AAAAAAAAAT".toCharArray, 23)), None, Some(FoundBarcode("AAA".toCharArray, 0)), None)

    consumer.consume(barcodes)
    val state = consumer.state

    for
      r <- rowReference.allBarcodes
      c <- colReference.allBarcodes
    do assertEquals(state.known.count((r, c)), 0L)

    assertEquals(state.knownCol.count("AAA"), 1L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 0L)
    assertEquals(state.matches, 0L)
    assertEquals(state.rowBarcodeNotFound, 0L)
    assertEquals(state.rowBarcodeStats.min, 23)
    assertEquals(state.rowBarcodeStats.max, 23)
  }

  test("found matching row") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, None, None, false)
    val barcodes =
      Barcodes(Some(FoundBarcode("AAAAAAAAAA".toCharArray, 23)), None, Some(FoundBarcode("AAT".toCharArray, 0)), None)

    consumer.consume(barcodes)
    val state = consumer.state

    colReference.allBarcodes.foreach { c =>
      assertEquals(state.knownCol.count(c), 0L)
      rowReference.allBarcodes.foreach(r => assertEquals(state.known.count((r, c)), 0L))
    }
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 0L)
    assertEquals(state.matches, 0L)
    assertEquals(state.rowBarcodeNotFound, 0L)
    assertEquals(state.rowBarcodeStats.min, 23)
    assertEquals(state.rowBarcodeStats.max, 23)
  }

  test("found unknown row and column") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, None, None, false)
    val barcodes =
      Barcodes(Some(FoundBarcode("AAAAAAAAAT".toCharArray, 23)), None, Some(FoundBarcode("AAT".toCharArray, 0)), None)

    consumer.consume(barcodes)
    val state = consumer.state

    for
      r <- rowReference.allBarcodes
      c <- colReference.allBarcodes
    do assertEquals(state.known.count((r, c)), 0L)

    assertEquals(state.knownCol.count("AAA"), 0L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 0L)
    assertEquals(state.matches, 0L)
    assertEquals(state.rowBarcodeNotFound, 0L)
    assertEquals(state.rowBarcodeStats.min, 23)
    assertEquals(state.rowBarcodeStats.max, 23)
  }

  test("found column but no row") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, None, None, false)
    val barcodes = Barcodes(None, None, Some(FoundBarcode("AAA".toCharArray, 0)), None)

    consumer.consume(barcodes)
    val state = consumer.state

    for
      r <- rowReference.allBarcodes
      c <- colReference.allBarcodes
    do assertEquals(state.known.count((r, c)), 0L)

    assertEquals(state.knownCol.count("AAA"), 0L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 0L)
    assertEquals(state.matches, 0L)
    assertEquals(state.rowBarcodeNotFound, 1L)
    assertEquals(state.rowBarcodeStats.min, Int.MaxValue)
    assertEquals(state.rowBarcodeStats.max, -1)
  }

  test("found column but no row (xy mode)") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, true, None, None, false)
    val barcodes = Barcodes(None, None, Some(FoundBarcode("AAA".toCharArray, 0)), None)

    consumer.consume(barcodes)
    val state = consumer.state

    for
      r <- rowReference.allBarcodes
      c <- colReference.allBarcodes
    do assertEquals(state.known.count((r, c)), 0L)

    assertEquals(state.knownCol.count("AAA"), 1L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 0L)
    assertEquals(state.matches, 0L)
    assertEquals(state.rowBarcodeNotFound, 1L)
    assertEquals(state.rowBarcodeStats.min, Int.MaxValue)
    assertEquals(state.rowBarcodeStats.max, -1)
  }

  test("found row barcode but no column barcode") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, None, None, false)
    val barcodes = Barcodes(Some(FoundBarcode("AAAAAAAAAT".toCharArray, 19)), None, None, None)

    consumer.consume(barcodes)
    val state = consumer.state

    for
      r <- rowReference.allBarcodes
      c <- colReference.allBarcodes
    do assertEquals(state.known.count((r, c)), 0L)

    assertEquals(state.knownCol.count("AAA"), 0L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 0L)
    assertEquals(state.matches, 0L)
    assertEquals(state.rowBarcodeNotFound, 0L)
    assertEquals(state.rowBarcodeStats.min, 19)
    assertEquals(state.rowBarcodeStats.max, 19)
  }

  test("found neither row barcode nor column barcode") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, None, None, false)
    val barcodes = Barcodes(None, None, None, None)

    consumer.consume(barcodes)
    val state = consumer.state

    for
      r <- rowReference.allBarcodes
      c <- colReference.allBarcodes
    do assertEquals(state.known.count((r, c)), 0L)

    assertEquals(state.knownCol.count("AAA"), 0L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 0L)
    assertEquals(state.matches, 0L)
    assertEquals(state.rowBarcodeNotFound, 1L)
    assertEquals(state.rowBarcodeStats.min, Int.MaxValue)
    assertEquals(state.rowBarcodeStats.max, -1)
  }

  test("umi barcodes") {
    val umiReference: BarcodeSet = new BarcodeSet(Set("AAAA"))
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, Some(umiReference), None, false)
    val barcodes =
      Barcodes(
        Some(FoundBarcode("AAAAAAAAAA".toCharArray, 23)),
        None,
        Some(FoundBarcode("AAA".toCharArray, 0)),
        Some(FoundBarcode("AAAA".toCharArray, 42))
      )

    consumer.consume(barcodes)
    val state = consumer.state

    assertEquals(state.known.count(("AAAAAAAAAA", "AAA")), 1L)
    assertEquals(state.knownCol.count("AAA"), 1L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 1L)
    assertEquals(state.matches, 1L)
    assertEquals(state.rowBarcodeNotFound, 0L)
    assertEquals(state.rowBarcodeStats.min, 23)
    assertEquals(state.rowBarcodeStats.max, 23)
    assertEquals(state.unknownUmi.keys, Set.empty[String])
  }

  test("unknown umi barcodes") {
    val umiReference: BarcodeSet = new BarcodeSet(Set("AAAA"))
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, Some(umiReference), None, false)
    val barcodes =
      Barcodes(
        Some(FoundBarcode("AAAAAAAAAA".toCharArray, 23)),
        None,
        Some(FoundBarcode("AAA".toCharArray, 0)),
        Some(FoundBarcode("TTTT".toCharArray, 42))
      )

    consumer.consume(barcodes)
    val state = consumer.state

    // we still count it
    val t = ("AAAAAAAAAA", "AAA")
    assertEquals(state.known.count(t), 1L)
    // but it goes in the catch-all shard
    state.known.shards.foreach(shard => assertEquals(state.known.forShard(Some(shard)).count(t), 0L))
    assertEquals(state.knownCol.count("AAA"), 1L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 1L)
    assertEquals(state.matches, 1L)
    assertEquals(state.rowBarcodeNotFound, 0L)
    assertEquals(state.rowBarcodeStats.min, 23)
    assertEquals(state.rowBarcodeStats.max, 23)
    assertEquals(state.unknownUmi.keys, Set("TTTT"))
  }

  test("paired end sequencing both construct barcodes found") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, None, None, true)
    val barcodes =
      Barcodes(
        Some(FoundBarcode("AAAAAAA".toCharArray, 23)),
        Some(FoundBarcode("AAA".toCharArray(), 20)),
        Some(FoundBarcode("AAA".toCharArray, 0)),
        None
      )

    consumer.consume(barcodes)
    val state = consumer.state

    assertEquals(state.known.count(("AAAAAAAAAA", "AAA")), 1L)
    assertEquals(state.knownCol.count("AAA"), 1L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 1L)
    assertEquals(state.matches, 1L)
    assertEquals(state.neitherRowBarcodeFound, 0L)
    assertEquals(state.rowBarcodeNotFound, 0L)
    assertEquals(state.rowBarcodeStats.min, 23)
    assertEquals(state.rowBarcodeStats.max, 23)
    assertEquals(state.revRowBarcodeNotFound, 0L)
    assertEquals(state.revRowBarcodeStats.min, 20)
    assertEquals(state.revRowBarcodeStats.max, 20)
  }

  test("paired end sequencing only reverse found") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, None, None, true)
    val barcodes =
      Barcodes(None, Some(FoundBarcode("AAA".toCharArray(), 20)), Some(FoundBarcode("AAA".toCharArray, 0)), None)

    consumer.consume(barcodes)
    val state = consumer.state

    assertEquals(state.known.count(("AAAAAAAAAA", "AAA")), 0L)
    // in XY-compatibility mode this would be 1
    assertEquals(state.knownCol.count("AAA"), 0L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 0L)
    assertEquals(state.matches, 0L)
    assertEquals(state.neitherRowBarcodeFound, 0L)
    assertEquals(state.rowBarcodeNotFound, 1L)
    assertEquals(state.rowBarcodeStats.min, Int.MaxValue)
    assertEquals(state.rowBarcodeStats.max, -1)
    assertEquals(state.revRowBarcodeNotFound, 0L)
    assertEquals(state.revRowBarcodeStats.min, 20)
    assertEquals(state.revRowBarcodeStats.max, 20)
  }

  test("paired end sequencing only forward found") {
    val consumer = new ScoringConsumer(rowReference, colReference, false, false, None, None, true)
    val barcodes =
      Barcodes(Some(FoundBarcode("AAAAA".toCharArray(), 8)), None, Some(FoundBarcode("AAA".toCharArray, 0)), None)

    consumer.consume(barcodes)
    val state = consumer.state

    assertEquals(state.known.count(("AAAAAAAAAA", "AAA")), 0L)
    // in XY-compatibility mode this would be 1
    assertEquals(state.knownCol.count("AAA"), 0L)
    assertEquals(state.reads, 1L)
    assertEquals(state.exactMatches, 0L)
    assertEquals(state.matches, 0L)
    assertEquals(state.neitherRowBarcodeFound, 0L)
    assertEquals(state.rowBarcodeNotFound, 0L)
    assertEquals(state.rowBarcodeStats.min, 8)
    assertEquals(state.rowBarcodeStats.max, 8)
    assertEquals(state.revRowBarcodeNotFound, 1L)
    assertEquals(state.revRowBarcodeStats.min, Int.MaxValue)
    assertEquals(state.revRowBarcodeStats.max, -1)
  }

end ScoringConsumerTest
