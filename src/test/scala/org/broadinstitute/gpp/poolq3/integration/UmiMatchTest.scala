/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
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
  private val brdn01 = "AAAAAAAAAAAAAAAAAAAA"
  private val brdn02 = "AAAAAAAAAAAAAAAAAAAC"
  private val brdn03 = "AAAAAAAAAAAAAAAAAAAG"
  private val brdn04 = "AAAAAAAAAAAAAAAAAAAT"
  private val brdn05 = "GATGTGCAGTGAGTAGCGAG"
  private val brdn06 = "CCGGTTGATGCGTGGTGATG"
  private val brdn07 = "AATGTGAAAATGTGATGAAT"

  private val rowReferenceBarcodes =
    List(brdn01, brdn02, brdn03, brdn04, brdn05, brdn06, brdn07).map(b => ReferenceEntry(b, b))

  // column barcodes
  private val eh1 = "AAAA"
  private val eh2 = "AAAT"
  private val sea1 = "CCCC"
  private val sea2 = "CCCG"

  private val colReferenceBarcodes =
    List(ReferenceEntry(eh1, "Eh"), ReferenceEntry(eh2, "Eh"), ReferenceEntry(sea1, "Sea"), ReferenceEntry(sea2, "Sea"))

  // umi barcodes
  private val fake = "TTTTT"
  private val a01 = "GAAAA"
  private val a03 = "GCCCC"
  private val e09 = "GTTTT"
  private val f02 = "AGGGG"
  private val umiBarcodes = new BarcodeSet(Set(a01, a03, e09, f02))

  private val rowReference = ExactReference(rowReferenceBarcodes, identity, includeAmbiguous = true)
  private val colReference = ExactReference(colReferenceBarcodes, identity, includeAmbiguous = false)

  private val expectedCounts: Map[(Option[String], Option[String], Option[String]), Long] = Map(
    (None, None, None) -> 2L,
    (brdn01.some, sea1.some, None) -> 1L,
    (brdn01.some, sea1.some, fake.some) -> 3L,
    (brdn01.some, sea2.some, e09.some) -> 9L,
    (brdn02.some, eh1.some, f02.some) -> 11L,
    (brdn03.some, sea1.some, e09.some) -> 6L,
    (brdn04.some, eh2.some, a01.some) -> 8L,
    (brdn06.some, sea2.some, a01.some) -> 13L,
    (brdn07.some, eh1.some, e09.some) -> 5L
  )

  private val barcodes = CloseableIterable.ofList {
    val l = expectedCounts.flatMap { case ((ro, co, uo), counts) =>
      val bcs =
        Barcodes(
          ro.map(r => FoundBarcode(r.toCharArray, 0)),
          None,
          co.map(c => FoundBarcode(c.toCharArray, 0)),
          uo.map(u => FoundBarcode(u.toCharArray, 24))
        )
      // none of the test counts will overflow so `.toInt` is safe
      List.fill(counts.toInt)(bcs)
    }
    Random.shuffle(l.toList)
  }

  test("PoolQ should process reads with UMI barcodes") {
    val consumer =
      new ScoringConsumer(rowReference, colReference, countAmbiguous = true, false, Some(umiBarcodes), None, false)

    val ret = PoolQ.runProcess(barcodes, consumer)
    val state = ret.get.state

    assertEquals(state.reads, 58L)
    assertEquals(state.exactMatches, 56L)

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
