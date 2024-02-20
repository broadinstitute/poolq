/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, CloseableIterator}
import org.broadinstitute.gpp.poolq3.types.Read

final class SingleFileBarcodeSource(
  parser: CloseableIterable[Read],
  rowPolicy: BarcodePolicy,
  columnPolicy: BarcodePolicy,
  umiPolicyOpt: Option[BarcodePolicy]
) extends CloseableIterable[Barcodes]:

  private[this] class BarcodeIterator(iterator: CloseableIterator[Read]) extends CloseableIterator[Barcodes]:
    override def hasNext: Boolean = iterator.hasNext

    override def next(): Barcodes =
      val nextRead = iterator.next()
      val rowBarcodeOpt = rowPolicy.find(nextRead)
      val colBarcodeOpt = columnPolicy.find(nextRead)
      val umiBarcodeOpt = umiPolicyOpt.flatMap(_.find(nextRead))
      Barcodes(rowBarcodeOpt, None, colBarcodeOpt, umiBarcodeOpt)

    override def close(): Unit = iterator.close()

  end BarcodeIterator

  override def iterator: CloseableIterator[Barcodes] =
    new BarcodeIterator(parser.iterator)

end SingleFileBarcodeSource
