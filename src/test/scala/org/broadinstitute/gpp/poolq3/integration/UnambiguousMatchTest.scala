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

class UnambiguousMatchTest extends AnyFlatSpec {

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

  private val rowReference = VariantReference(rowReferenceBarcodes, identity, includeAmbiguous = false)
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

  "PoolQ" should "not count ambiguous bases" in {
    val consumer = new ScoringConsumer(rowReference, colReference, countAmbiguous = true, false, None, None, false)

    val ret = PoolQ.runProcess(barcodes, consumer)
    val state = ret.get.state

    state.reads should be(9)
    state.exactMatches should be(3)

    val hist = state.known
    hist.count(("AAAAAAAAAAAAAAAAAAAA", "AAAA")) should be(1)
    hist.count(("AAAAAAAAAAAAAAAAAAAA", "AAAT")) should be(1)
    hist.count(("GATGTGCAGTGAGTAGCGAG", "AAAA")) should be(1)
    hist.count(("CCGGTTGATGCGTGGTGATG", "CCCC")) should be(1)
    hist.count(("AATGTGAAAATGTGATGAAT", "CCCG")) should be(1)

    // these all correspond to the ambiguous match at the end
    hist.count(("AAAAAAAAAAAAAAAAAAAA", "CCCC")) should be(0)
    hist.count(("AAAAAAAAAAAAAAAAAAAC", "CCCC")) should be(0)
    hist.count(("AAAAAAAAAAAAAAAAAAAG", "CCCC")) should be(0)
    hist.count(("AAAAAAAAAAAAAAAAAAAT", "CCCC")) should be(0)

    // these are combinations that didn't occur
    hist.count(("AAAAAAAAAAAAAAAAAAAA", "CCCG")) should be(0)

    hist.count(("AAAAAAAAAAAAAAAAAAAC", "AAAA")) should be(0)
    hist.count(("AAAAAAAAAAAAAAAAAAAC", "AAAT")) should be(0)
    hist.count(("AAAAAAAAAAAAAAAAAAAC", "CCCG")) should be(0)

    hist.count(("AAAAAAAAAAAAAAAAAAAG", "AAAA")) should be(0)
    hist.count(("AAAAAAAAAAAAAAAAAAAG", "AAAT")) should be(0)
    hist.count(("AAAAAAAAAAAAAAAAAAAG", "CCCG")) should be(0)

    hist.count(("AAAAAAAAAAAAAAAAAAAT", "AAAA")) should be(0)
    hist.count(("AAAAAAAAAAAAAAAAAAAT", "AAAT")) should be(0)
    hist.count(("AAAAAAAAAAAAAAAAAAAT", "CCCG")) should be(0)

    hist.count(("GATGTGCAGTGAGTAGCGAG", "AAAT")) should be(0)
    hist.count(("GATGTGCAGTGAGTAGCGAG", "CCCC")) should be(0)
    hist.count(("GATGTGCAGTGAGTAGCGAG", "CCCG")) should be(0)

    hist.count(("AATGTGAAAATGTGATGAAT", "AAAA")) should be(0)
    hist.count(("AATGTGAAAATGTGATGAAT", "AAAT")) should be(0)
    hist.count(("AATGTGAAAATGTGATGAAT", "CCCC")) should be(0)
  }

}
