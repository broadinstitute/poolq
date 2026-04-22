/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.integration

import munit.FunSuite
import org.broadinstitute.gpp.poolq3.PoolQ
import org.broadinstitute.gpp.poolq3.barcode.{Barcodes, FoundBarcode}
import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, ReferenceEntry}
import org.broadinstitute.gpp.poolq3.process.ScoringConsumer
import org.broadinstitute.gpp.poolq3.reference.{ExactReference, VariantReference}

class AmbiguousMatchTest extends FunSuite:

  private val rowReferenceBarcodes = List(
    "AAAAAAAAAAAAAAAAAAAA",
    "AAAAAAAAAAAAAAAAAAAC",
    "AAAAAAAAAAAAAAAAAAAG",
    "AAAAAAAAAAAAAAAAAAAT",
    "GATGTGCAGTGAGTAGCGAG",
    "CCGGTTGATGCGTGGTGATG",
    "AATGTGAAAATGTGATGAAT"
  ).map(b => ReferenceEntry(b, b))

  private val colReferenceBarcodes = List(
    ReferenceEntry("AAAA", "Eh"),
    ReferenceEntry("AAAT", "Eh"),
    ReferenceEntry("CCCC", "Sea"),
    ReferenceEntry("CCCG", "Sea")
  )

  private val rowReference = VariantReference(rowReferenceBarcodes, identity, includeAmbiguous = true)
  private val colReference = ExactReference(colReferenceBarcodes, identity, includeAmbiguous = false)

  private val barcodes = CloseableIterable.ofList(
    List(
      (None, None),
      (Some("AAAAAAAAAAAAAAAAAAAA"), None),
      (None, Some("AAAA")),
      (Some("NAAAAAAAAAAAAAAAAAAA"), Some("AAAA")), // note N
      (Some("AAAAAAAAAAAAAAAAAAAA"), Some("AAAT")),
      (Some("GATGTNCAGTGAGTAGCGAG"), Some("AAAA")), // note N
      (Some("CCGGTTGATGCGTGGTGATG"), Some("CCCC")),
      (Some("AATGTGAAAATGTGATGAAT"), Some("CCCG")),
      (Some("AAAAAAAAAAAAAAAAAAAN"), Some("CCCC")) // note N yielding an ambiguous match
    ).map { case (rowOpt, colOpt) =>
      Barcodes(
        rowOpt.map(s => FoundBarcode(s.toCharArray, 0)),
        None,
        colOpt.map(s => FoundBarcode(s.toCharArray, 0)),
        None
      )
    }
  )

  test("PoolQ should count ambiguous bases") {
    val consumer = new ScoringConsumer(rowReference, colReference, countAmbiguous = true, false, None, None, false)

    val ret = PoolQ.runProcess(barcodes, consumer)
    val state = ret.get.state

    assertEquals(state.reads, 9L)
    assertEquals(state.exactMatches, 3L)

    val hist = state.known
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAA", "AAAA")), 1L)
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAA", "AAAT")), 1L)
    assertEquals(hist.count(("GATGTGCAGTGAGTAGCGAG", "AAAA")), 1L)
    assertEquals(hist.count(("CCGGTTGATGCGTGGTGATG", "CCCC")), 1L)
    assertEquals(hist.count(("AATGTGAAAATGTGATGAAT", "CCCG")), 1L)

    // these all correspond to the ambiguous match at the end
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAA", "CCCC")), 1L)
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAC", "CCCC")), 1L)
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAG", "CCCC")), 1L)
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAT", "CCCC")), 1L)

    // these are combinations that didn't occur
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAA", "CCCG")), 0L)

    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAC", "AAAA")), 0L)
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAC", "AAAT")), 0L)
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAC", "CCCG")), 0L)

    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAG", "AAAA")), 0L)
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAG", "AAAT")), 0L)
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAG", "CCCG")), 0L)

    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAT", "AAAA")), 0L)
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAT", "AAAT")), 0L)
    assertEquals(hist.count(("AAAAAAAAAAAAAAAAAAAT", "CCCG")), 0L)

    assertEquals(hist.count(("GATGTGCAGTGAGTAGCGAG", "AAAT")), 0L)
    assertEquals(hist.count(("GATGTGCAGTGAGTAGCGAG", "CCCC")), 0L)
    assertEquals(hist.count(("GATGTGCAGTGAGTAGCGAG", "CCCG")), 0L)

    assertEquals(hist.count(("AATGTGAAAATGTGATGAAT", "AAAA")), 0L)
    assertEquals(hist.count(("AATGTGAAAATGTGATGAAT", "AAAT")), 0L)
    assertEquals(hist.count(("AATGTGAAAATGTGATGAAT", "CCCC")), 0L)
  }

end AmbiguousMatchTest
