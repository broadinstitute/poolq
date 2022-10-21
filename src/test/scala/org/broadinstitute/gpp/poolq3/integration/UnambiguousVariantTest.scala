/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.integration

import org.broadinstitute.gpp.poolq3.PoolQ
import org.broadinstitute.gpp.poolq3.barcode.{Barcodes, FoundBarcode}
import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, ReferenceEntry}
import org.broadinstitute.gpp.poolq3.process.ScoringConsumer
import org.broadinstitute.gpp.poolq3.reference.{ExactReference, VariantReference}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class UnambiguousVariantTest extends AnyFlatSpec {

  private val rowReferenceBarcodes = List("AAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAC").map(b => ReferenceEntry(b, b))

  private val colReferenceBarcodes = List(
    ReferenceEntry("AAAA", "Eh"),
    ReferenceEntry("AAAT", "Eh"),
    ReferenceEntry("CCCC", "Sea"),
    ReferenceEntry("CCCG", "Sea")
  )

  private val rowReference = VariantReference(rowReferenceBarcodes, identity, includeAmbiguous = false)
  private val colReference = ExactReference(colReferenceBarcodes, identity, includeAmbiguous = false)

  private val input = List(("NAAAAAAAAAAAAAAAAAAA", "AAAA"))

  private val reads = CloseableIterable.ofList {
    input.map { case (row, col) =>
      Barcodes(Some(FoundBarcode(row.toCharArray, 0)), None, Some(FoundBarcode(col.toCharArray, 0)), None)
    }
  }

  "PoolQ" should "match a read with an N" in {
    val consumer = new ScoringConsumer(rowReference, colReference, countAmbiguous = true, false, None, None, false)

    val ret = PoolQ.runProcess(reads, consumer)
    val state = ret.get.state

    state.reads should be(1)
    state.exactMatches should be(0)

    val hist = state.known
    hist.count(("AAAAAAAAAAAAAAAAAAAA", "AAAA")) should be(1)
    for {
      row <- rowReferenceBarcodes.map(_.dnaBarcode)
      col <- colReferenceBarcodes.map(_.dnaBarcode)
    } {
      val expected = if (row.forall(_ == 'A') && col.forall(_ == 'A')) 1 else 0
      hist.count((row, col)) should be(expected)
    }
  }

}
