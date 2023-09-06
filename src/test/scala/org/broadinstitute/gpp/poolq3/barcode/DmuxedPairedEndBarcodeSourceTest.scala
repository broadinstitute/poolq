/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import cats.syntax.all._
import munit.FunSuite
import org.broadinstitute.gpp.poolq3.parser.DmuxedIterable
import org.broadinstitute.gpp.poolq3.types.ReadIdCheckPolicy

class DmuxedPairedEndBarcodeSourceTest extends FunSuite {

  private[this] val rowPolicy = BarcodePolicy("FIXED@0", 4, skipShortReads = true)
  private[this] val revRowPolicy = BarcodePolicy("FIXED@0", 3, skipShortReads = true)

  def fb(r1: String, r2: String) =
    Barcodes(FoundBarcode(r1.toCharArray, 0).some, FoundBarcode(r2.toCharArray, 0).some, None, None)

  def fb(i: String, r1: String, r2: String) =
    Barcodes(
      FoundBarcode(r1.toCharArray, 0).some,
      FoundBarcode(r2.toCharArray, 0).some,
      FoundBarcode(i.toCharArray, 0).some,
      None
    )

  test("it works") {
    val iter1 =
      DmuxedIterable(List(None -> List("AAAA", "CCCC", "GGGG"), Some("CTCGAG") -> List("TTAA", "CCGG", "AATT")))

    val iter2 = DmuxedIterable(List(None -> List("AGA", "CTC", "GAG"), Some("CTCGAG") -> List("TGT", "CAC", "TCT")))

    val src = new DmuxedPairedEndBarcodeSource(iter1, iter2, rowPolicy, revRowPolicy, None, ReadIdCheckPolicy.Lax)
    assertEquals(
      src.toList,
      List(
        fb("AAAA", "AGA"),
        fb("CCCC", "CTC"),
        fb("GGGG", "GAG"),
        fb("CTCGAG", "TTAA", "TGT"),
        fb("CTCGAG", "CCGG", "CAC"),
        fb("CTCGAG", "AATT", "TCT")
      )
    )
  }

  test("nothing works") {
    val i1 = DmuxedIterable(Nil)
    val i2 = DmuxedIterable(Nil)
    val src = new DmuxedPairedEndBarcodeSource(i1, i2, rowPolicy, revRowPolicy, None, ReadIdCheckPolicy.Illumina)
    assertEquals(src.toList, Nil)
  }

}
