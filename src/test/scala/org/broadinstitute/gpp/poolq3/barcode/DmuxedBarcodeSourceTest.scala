/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import cats.syntax.all._
import munit.FunSuite
import org.broadinstitute.gpp.poolq3.parser.DmuxedIterable

class DmuxedBarcodeSourceTest extends FunSuite {

  private[this] val rowPolicy = BarcodePolicy("FIXED@0", 10, skipShortReads = true)

  def fb(s: String) = Barcodes(FoundBarcode(s.toCharArray, 0).some, None, None, None)

  def fb(i: String, s: String) =
    Barcodes(FoundBarcode(s.toCharArray, 0).some, None, FoundBarcode(i.toCharArray, 0).some, None)

  test("it works") {
    val iterable = DmuxedIterable(
      List(
        None -> List("AAAAAAAAAA", "AAAAAAAAAC", "AAAAAAAAAG"),
        Some("CTCGAG") -> List("AAAAAAAAAA", "AACCCCGGTT", "AATTGGTTAA")
      )
    )

    val src = new DmuxedBarcodeSource(iterable, rowPolicy, None)
    assertEquals(
      src.toList,
      List(
        fb("AAAAAAAAAA"),
        fb("AAAAAAAAAC"),
        fb("AAAAAAAAAG"),
        fb("CTCGAG", "AAAAAAAAAA"),
        fb("CTCGAG", "AACCCCGGTT"),
        fb("CTCGAG", "AATTGGTTAA")
      )
    )
  }

  test("nothing works") {
    val iterable = DmuxedIterable(Nil)
    val src = new DmuxedBarcodeSource(iterable, rowPolicy, None)
    assertEquals(src.toList, Nil)
  }

}
