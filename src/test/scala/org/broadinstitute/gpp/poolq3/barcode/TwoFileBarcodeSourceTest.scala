/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import cats.syntax.all.*
import munit.FunSuite
import org.broadinstitute.gpp.poolq3.parser.CloseableIterable
import org.broadinstitute.gpp.poolq3.types.{Read, ReadIdCheckPolicy}

class TwoFileBarcodeSourceTest extends FunSuite:

  private[this] val rowPolicy = BarcodePolicy("FIXED@0", 10, skipShortReads = true)
  private[this] val colPolicy = BarcodePolicy("FIXED@0", 4, skipShortReads = true)
  private[this] val umiPolicy = BarcodePolicy("FIXED@10", 3, skipShortReads = true).some

  private[this] def seqsToReads(xs: List[String]): List[Read] =
    xs.zipWithIndex.map { case (seq, i) => Read(i.toString, seq) }

  test("iterator draws col, umi, and row barcodes from the correct reads") {
    val rowReads = CloseableIterable.ofList(seqsToReads(List("AAAAAAAAAATTTC")))
    val colReads = CloseableIterable.ofList(seqsToReads(List("GGGG")))

    val src = new TwoFileBarcodeSource(rowReads, colReads, rowPolicy, colPolicy, umiPolicy, ReadIdCheckPolicy.Strict)

    src.iterator.toList match
      case r :: Nil =>
        assertEquals(r.col.map(b => new String(b.barcode)), Some("GGGG"))
        assertEquals(r.row.map(b => new String(b.barcode)), Some("AAAAAAAAAA"))
        assertEquals(r.umi.map(b => new String(b.barcode)), Some("TTT"))
      case _ => fail("This should not happen")
  }

end TwoFileBarcodeSourceTest
