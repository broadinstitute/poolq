/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.integration

import scala.util.Random

import cats.syntax.all.*
import munit.FunSuite
import org.broadinstitute.gpp.poolq3.PoolQ
import org.broadinstitute.gpp.poolq3.barcode.{Barcodes, FoundBarcode}
import org.broadinstitute.gpp.poolq3.parser.{BarcodeSet, CloseableIterable, ReferenceEntry}
import org.broadinstitute.gpp.poolq3.process.ScoringConsumer
import org.broadinstitute.gpp.poolq3.reference.ExactReference

class UmiMatchTest extends FunSuite:

  // row barcodes
  private[this] val brdn01 = "AAAAAAAAAAAAAAAAAAAA"
  private[this] val brdn02 = "AAAAAAAAAAAAAAAAAAAC"
  private[this] val brdn03 = "AAAAAAAAAAAAAAAAAAAG"
  private[this] val brdn04 = "AAAAAAAAAAAAAAAAAAAT"
  private[this] val brdn05 = "GATGTGCAGTGAGTAGCGAG"
  private[this] val brdn06 = "CCGGTTGATGCGTGGTGATG"
  private[this] val brdn07 = "AATGTGAAAATGTGATGAAT"

  private[this] val rowReferenceBarcodes =
    List(brdn01, brdn02, brdn03, brdn04, brdn05, brdn06, brdn07).map(b => ReferenceEntry(b, b))

  // column barcodes
  private[this] val eh1 = "AAAA"
  private[this] val eh2 = "AAAT"
  private[this] val sea1 = "CCCC"
  private[this] val sea2 = "CCCG"

  private[this] val colReferenceBarcodes =
    List(ReferenceEntry(eh1, "Eh"), ReferenceEntry(eh2, "Eh"), ReferenceEntry(sea1, "Sea"), ReferenceEntry(sea2, "Sea"))

  // umi barcodes
  private[this] val fake = "TTTTT"
  private[this] val a01 = "GAAAA"
  private[this] val a03 = "GCCCC"
  private[this] val e09 = "GTTTT"
  private[this] val f02 = "AGGGG"
  private[this] val umiBarcodes = new BarcodeSet(Set(a01, a03, e09, f02))

  private[this] val rowReference = ExactReference(rowReferenceBarcodes, identity, includeAmbiguous = true)
  private[this] val colReference = ExactReference(colReferenceBarcodes, identity, includeAmbiguous = false)

  private[this] val expectedCounts: Map[(Option[String], Option[String], Option[String]), Int] = Map(
    (None, None, None) -> 2,
    (brdn01.some, sea1.some, None) -> 1,
    (brdn01.some, sea1.some, fake.some) -> 3,
    (brdn01.some, sea2.some, e09.some) -> 9,
    (brdn02.some, eh1.some, f02.some) -> 11,
    (brdn03.some, sea1.some, e09.some) -> 6,
    (brdn04.some, eh2.some, a01.some) -> 8,
    (brdn06.some, sea2.some, a01.some) -> 13,
    (brdn07.some, eh1.some, e09.some) -> 5
  )

  private[this] val barcodes = CloseableIterable.ofList {
    val l = expectedCounts.flatMap { case ((ro, co, uo), counts) =>
      val bcs =
        Barcodes(
          ro.map(r => FoundBarcode(r.toCharArray, 0)),
          None,
          co.map(c => FoundBarcode(c.toCharArray, 0)),
          uo.map(u => FoundBarcode(u.toCharArray, 24))
        )
      List.fill(counts)(bcs)
    }
    Random.shuffle(l.toList)
  }

  test("PoolQ should process reads with UMI barcodes") {
    val consumer =
      new ScoringConsumer(rowReference, colReference, countAmbiguous = true, false, Some(umiBarcodes), None, false)

    val ret = PoolQ.runProcess(barcodes, consumer)
    val state = ret.get.state

    assertEquals(state.reads, 58)
    assertEquals(state.exactMatches, 56)

    val hist = state.known
    for
      row <- rowReference.allBarcodes
      col <- colReference.allBarcodes
      umi <- umiBarcodes.barcodes
      tuple = (row.some, col.some, umi.some)
      expectedTupleCount = expectedCounts.getOrElse(tuple, 0)
    do assertEquals(hist.forShard(umi.some).count((row, col)), expectedTupleCount)

  }

end UmiMatchTest
