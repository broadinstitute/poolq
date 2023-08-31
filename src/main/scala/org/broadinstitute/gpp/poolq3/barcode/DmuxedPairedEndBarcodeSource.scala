/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, CloseableIterator, DmuxedIterable}
import org.broadinstitute.gpp.poolq3.types.{Read, ReadIdCheckPolicy}

class DmuxedPairedEndBarcodeSource(
  rowParser: DmuxedIterable,
  revRowParser: DmuxedIterable,
  rowPolicy: BarcodePolicy,
  revRowPolicy: BarcodePolicy,
  umiPolicyOpt: Option[BarcodePolicy],
  readIdCheckPolicy: ReadIdCheckPolicy
) extends CloseableIterable[Barcodes] {

  private def colBarcodeOpt = rowParser.indexBarcode.map(bc => FoundBarcode(bc.toCharArray, 0))

  private[this] class BarcodeIterator(rowIterator: CloseableIterator[Read], revRowIterator: CloseableIterator[Read])
      extends CloseableIterator[Barcodes] {

    final override def hasNext: Boolean = rowIterator.hasNext && revRowIterator.hasNext

    final override def next(): Barcodes = {
      val nextRow = rowIterator.next()
      val nextRevRow = revRowIterator.next()
      readIdCheckPolicy.check(nextRow, nextRevRow)
      val rowBarcodeOpt = rowPolicy.find(nextRow)
      val revRowBarcodeOpt = revRowPolicy.find(nextRevRow)
      val umiBarcodeOpt = umiPolicyOpt.flatMap(_.find(nextRow))
      Barcodes(rowBarcodeOpt, revRowBarcodeOpt, colBarcodeOpt, umiBarcodeOpt)
    }

    final override def close(): Unit =
      try rowIterator.close()
      finally revRowIterator.close()

  }

  override def iterator: CloseableIterator[Barcodes] =
    new BarcodeIterator(rowParser.iterator, revRowParser.iterator)

}
