/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.integration

import scala.util.Random

import munit.FunSuite
import org.broadinstitute.gpp.poolq3.PoolQ
import org.broadinstitute.gpp.poolq3.barcode.{Barcodes, FoundBarcode}
import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, ReferenceEntry}
import org.broadinstitute.gpp.poolq3.process.ScoringConsumer
import org.broadinstitute.gpp.poolq3.reference.ExactReference

class PairedEndMatchTest extends FunSuite {

  // row barcodes
  private val r1 = "TTTCCC"
  private val r2 = "AAAGGG"
  private val r3 = "CCCGGG"
  private val r4 = "GGGTTT"

  private val rowReferenceBarcodes = List(r1, r2, r3, r4).map(b => ReferenceEntry(b, b))

  // column barcodes
  private val c1 = "A"
  private val c2 = "C"
  private val c3 = "G"

  private val colReferenceBarcodes =
    List(ReferenceEntry(c1, "Left"), ReferenceEntry(c2, "Right"), ReferenceEntry(c3, "Left"))

  // reference
  private val rowReference = ExactReference(rowReferenceBarcodes, identity, includeAmbiguous = false)
  private val colReference = ExactReference(colReferenceBarcodes, identity, includeAmbiguous = false)

  private val expectedCounts: Map[(Option[String], Option[String]), Int] = Map(
    // found nothing
    (None, None) -> 4,
    // found row
    (Some(r1), None) -> 3,
    (Some(r3), None) -> 2,
    // found column
    (None, Some(c2)) -> 6,
    //  found both
    (Some(r1), Some(c1)) -> 9,
    (Some(r1), Some(c2)) -> 6,
    (Some(r1), Some(c3)) -> 12,
    (Some(r2), Some(c1)) -> 10,
    (Some(r2), Some(c2)) -> 2,
    (Some(r2), Some(c3)) -> 8,
    (Some(r3), Some(c1)) -> 11,
    (Some(r3), Some(c2)) -> 7,
    (Some(r3), Some(c3)) -> 13,
    (Some(r4), Some(c1)) -> 16,
    (Some(r4), Some(c2)) -> 3,
    (Some(r4), Some(c3)) -> 5
  )

  private val barcodes = CloseableIterable.ofList {
    val l = expectedCounts.flatMap { case ((ro, co), counts) =>
      val bcs =
        Barcodes(
          ro.map(r => FoundBarcode(r.substring(0, 3).toCharArray, 0)),
          ro.map(r => FoundBarcode(r.substring(3).toCharArray(), 0)),
          co.map(c => FoundBarcode(c.toCharArray, 0)),
          None
        )
      List.fill(counts)(bcs)
    }
    Random.shuffle(l.toList)
  }

  test("PoolQ should process reads paired end sequencing") {
    val consumer =
      new ScoringConsumer(rowReference, colReference, countAmbiguous = false, false, None, None, true)

    val ret = PoolQ.runProcess(barcodes, consumer)
    val state = ret.get.state

    assertEquals(state.reads, 117)
    assertEquals(state.exactMatches, 102)

    val hist = state.known
    for
      row <- rowReference.allBarcodes
      col <- colReference.allBarcodes
      tuple = (Some(row), Some(col))
      expectedTupleCount = expectedCounts.getOrElse(tuple, 0)
    do {
      assertEquals(hist.forShard(None).count((row, col)), expectedTupleCount)
    }

  }

}
