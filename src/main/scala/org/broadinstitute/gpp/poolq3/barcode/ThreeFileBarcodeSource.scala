/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, CloseableIterator}
import org.broadinstitute.gpp.poolq3.types.{Read, ReadIdCheckPolicy}

final class ThreeFileBarcodeSource(
  rowParser: CloseableIterable[Read],
  revRowParser: CloseableIterable[Read],
  colParser: CloseableIterable[Read],
  rowPolicy: BarcodePolicy,
  revRowPolicy: BarcodePolicy,
  columnPolicy: BarcodePolicy,
  umiPolicyOpt: Option[BarcodePolicy],
  readIdCheckPolicy: ReadIdCheckPolicy
) extends CloseableIterable[Barcodes] {

  private[this] class BarcodeIterator(
    rowIterator: CloseableIterator[Read],
    revRowIterator: CloseableIterator[Read],
    colIterator: CloseableIterator[Read]
  ) extends CloseableIterator[Barcodes] {

    final override def hasNext: Boolean = rowIterator.hasNext && revRowIterator.hasNext && colIterator.hasNext

    final override def next(): Barcodes = {
      val nextRow = rowIterator.next()
      val nextRevRow = revRowIterator.next()
      val nextCol = colIterator.next()
      readIdCheckPolicy.check(nextRow, nextRevRow)
      readIdCheckPolicy.check(nextRow, nextCol)
      val rowBarcodeOpt = rowPolicy.find(nextRow)
      val revRowBarcodeOpt = revRowPolicy.find(nextRevRow)
      val colBarcodeOpt = columnPolicy.find(nextCol)
      val umiBarcodeOpt = umiPolicyOpt.flatMap(_.find(nextRow))
      Barcodes(rowBarcodeOpt, revRowBarcodeOpt, colBarcodeOpt, umiBarcodeOpt)
    }

    final override def close(): Unit =
      try rowIterator.close()
      finally colIterator.close()

  }

  override def iterator: CloseableIterator[Barcodes] =
    new BarcodeIterator(rowParser.iterator, revRowParser.iterator, colParser.iterator)

}
