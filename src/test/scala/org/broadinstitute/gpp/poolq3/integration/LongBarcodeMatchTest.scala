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

class LongBarcodeMatchTest extends FunSuite:

  private val rowReferenceBarcodes = List(
    ReferenceEntry(
      "TTTCTGTCATCCAAATACTCCACACGCAAATTTCCTTCCACTCGGATAAGATGCTGAGGAGGGGCCAGACCTAAGAGCAATCAGTGAGGAATCAGAGGCCTGGGGACCCTGGGCAACCAGCCCTGTCGTCTCTCCAGCCCCAGC",
      "A Long Barcode"
    )
  )

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
      (
        Some(
          "TTTCTGTCATCCAAATACTCCACACGCAAATTTCCTTCCACTCGGATAAGATGCTGAGGAGGGGCCAGACCTAAGAGCAATCAGTGAGGAATCAGAGGCCTGGGGACCCTGGGCAACCAGCCCTGTCGTCTCTCCAGCCCCAGC"
        ),
        None
      ),
      (None, Some("AAAA")),
      (
        Some(
          "TTTCTGTCATCCAAATACTCCACACGCAAATTTCCTTCCACTCGGATAAGATGCTGAGGAGGGGCCAGACCTAAGAGCAATCAGNGAGGAATCAGAGGCCTGGGGACCCTGGGCAACCAGCCCTGTCGTCTCTCCAGCCCCAGC"
        ),
        Some("AAAA")
      ), // note N, matched anyway
      (
        Some(
          "TTTCTGTCATCCAAATACTCCACACGCAAATTTCCTTCCACTCGGATAAGATGCTGAGGAGGGGCCAGACCTAAGAGCAATCAGTGAGGAATCAGAGGCCTGGGGACCCTGGGCAACCAGCCCTGTCGTCTCTCCAGCCCCAGC"
        ),
        Some("AAAT")
      ), // exact match
      (
        Some(
          "NAGCGACTGATGCTGATGACGCATGTAGTAGTGCACTGATGATGTTTTTAGTGTGACCCCGATGTAGCCGATGCCCAAAATGATGCATGACGTAGTAGTAGCAGATGATGACGTAGTAGTAGAATTGGCCCGATGTCCGCCTGA"
        ),
        Some("AAAA")
      ), // note N, unknown seq
      (
        Some(
          "CCGGTTGATGCGTGGTGATGCCGGTTGATGCGTGGTGATGCCGGTTGATGCGTGGTGATGCCGGTTGATGCGTGGTGATGCCGGTTGATGCGTGGTGATGCCGGTTGATGCGTGGTGATGCCGGTTGATGCGTGGTGATGTTGA"
        ),
        Some("CCCC")
      ), // unknown seq
      (
        Some(
          "ATTCTGTCATCCAAATACTCCACACGCAAATTTCCTTCCACTCGGATAAGATGCTGAGGAGGGGCCAGACCTAAGAGCAATCAGTGAGGAATCAGAGGCCTGGGGACCCTGGGCAACCAGCCCTGTCGTCTCTCCAGCCCCAGC"
        ),
        Some("CCCG")
      ), // note leading A, matched anyway
      (
        Some(
          "TTTCTGTCATCCAAATACTCCACACGCAAATTTCCTTCCACTCGGATAAGATGCTGAGGAGGGGCCAGACCTAAGAGCAATCAGTGAGGAATCAGAGGCCTGGGGACCCTGGGCAACCAGCCCTGTCGTCTCTCCAGCCCCAGC"
        ),
        Some("CCCC")
      ) // exact match
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
    assertEquals(state.exactMatches, 2L)
    assertEquals(state.matches, 4L)

    val hist = state.known

    // these are all the matches
    assertEquals(
      hist.count(
        (
          "TTTCTGTCATCCAAATACTCCACACGCAAATTTCCTTCCACTCGGATAAGATGCTGAGGAGGGGCCAGACCTAAGAGCAATCAGTGAGGAATCAGAGGCCTGGGGACCCTGGGCAACCAGCCCTGTCGTCTCTCCAGCCCCAGC",
          "AAAT"
        )
      ),
      1L
    )
    assertEquals(
      hist.count(
        (
          "TTTCTGTCATCCAAATACTCCACACGCAAATTTCCTTCCACTCGGATAAGATGCTGAGGAGGGGCCAGACCTAAGAGCAATCAGTGAGGAATCAGAGGCCTGGGGACCCTGGGCAACCAGCCCTGTCGTCTCTCCAGCCCCAGC",
          "AAAA"
        )
      ),
      1L
    )
    assertEquals(
      hist.count(
        (
          "TTTCTGTCATCCAAATACTCCACACGCAAATTTCCTTCCACTCGGATAAGATGCTGAGGAGGGGCCAGACCTAAGAGCAATCAGTGAGGAATCAGAGGCCTGGGGACCCTGGGCAACCAGCCCTGTCGTCTCTCCAGCCCCAGC",
          "CCCG"
        )
      ),
      1L
    )
    assertEquals(
      hist.count(
        (
          "TTTCTGTCATCCAAATACTCCACACGCAAATTTCCTTCCACTCGGATAAGATGCTGAGGAGGGGCCAGACCTAAGAGCAATCAGTGAGGAATCAGAGGCCTGGGGACCCTGGGCAACCAGCCCTGTCGTCTCTCCAGCCCCAGC",
          "CCCC"
        )
      ),
      1L
    )
  }

end LongBarcodeMatchTest
