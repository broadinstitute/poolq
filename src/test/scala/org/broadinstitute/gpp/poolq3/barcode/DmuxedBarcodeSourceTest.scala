/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import cats.syntax.all.*
import munit.FunSuite
import org.broadinstitute.gpp.poolq3.parser.DmuxedIterable
import org.broadinstitute.gpp.poolq3.types.Read

class DmuxedBarcodeSourceTest extends FunSuite:

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

    val src = new DmuxedBarcodeSource(iterable, rowPolicy, None, 8)
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

  test("barcodes from read IDs") {
    val undeterminedReads = List(Read("@eeeeee ACGTAA", "AAAAAAAAAA"), Read("@eeeeee ACTCAG", "CCCCCCCCCC"))
    val aacctgReads = List(Read("@a read", "GGGGGGGGGG"), Read("@another read", "TTTTTTTTTT"))
    val iterable = DmuxedIterable.forReads(List(None -> undeterminedReads, Some("AACCTG") -> aacctgReads))

    val src = new DmuxedBarcodeSource(iterable, rowPolicy, None, 6)
    assertEquals(
      src.toList,
      List(
        fb("ACGTAA", "AAAAAAAAAA"),
        fb("ACTCAG", "CCCCCCCCCC"),
        fb("AACCTG", "GGGGGGGGGG"),
        fb("AACCTG", "TTTTTTTTTT")
      )
    )
  }

  test("nothing works") {
    val iterable = DmuxedIterable(Nil)
    val src = new DmuxedBarcodeSource(iterable, rowPolicy, None, 8)
    assertEquals(src.toList, Nil)
  }

end DmuxedBarcodeSourceTest
