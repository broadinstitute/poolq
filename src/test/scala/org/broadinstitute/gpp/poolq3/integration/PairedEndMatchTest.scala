/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
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

class PairedEndMatchTest extends FunSuite:

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

  private val expectedCounts: Map[(Option[String], Option[String]), Long] = Map(
    // found nothing
    (None, None) -> 4L,
    // found row
    (Some(r1), None) -> 3L,
    (Some(r3), None) -> 2L,
    // found column
    (None, Some(c2)) -> 6L,
    //  found both
    (Some(r1), Some(c1)) -> 9L,
    (Some(r1), Some(c2)) -> 6L,
    (Some(r1), Some(c3)) -> 12L,
    (Some(r2), Some(c1)) -> 10L,
    (Some(r2), Some(c2)) -> 2L,
    (Some(r2), Some(c3)) -> 8L,
    (Some(r3), Some(c1)) -> 11L,
    (Some(r3), Some(c2)) -> 7L,
    (Some(r3), Some(c3)) -> 13L,
    (Some(r4), Some(c1)) -> 16L,
    (Some(r4), Some(c2)) -> 3L,
    (Some(r4), Some(c3)) -> 5L
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
      // none of these test counts will overflow so `.toInt` is safe
      List.fill(counts.toInt)(bcs)
    }
    Random.shuffle(l.toList)
  }

  test("PoolQ should process reads paired end sequencing") {
    val consumer =
      new ScoringConsumer(rowReference, colReference, countAmbiguous = false, false, None, None, true)

    val ret = PoolQ.runProcess(barcodes, consumer)
    val state = ret.get.state

    assertEquals(state.reads, 117L)
    assertEquals(state.exactMatches, 102L)

    val hist = state.known
    for
      row <- rowReference.allBarcodes
      col <- colReference.allBarcodes
      tuple = (Some(row), Some(col))
      expectedTupleCount = expectedCounts.getOrElse(tuple, 0L)
    do assertEquals(hist.forShard(None).count((row, col)), expectedTupleCount)

  }

end PairedEndMatchTest
