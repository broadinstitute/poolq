/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, CloseableIterator, DmuxedIterable}
import org.broadinstitute.gpp.poolq3.types.Read

final class DmuxedBarcodeSource(
  parser: DmuxedIterable,
  rowPolicy: BarcodePolicy,
  umiPolicyOpt: Option[BarcodePolicy],
  colBarcodeLength: Int
) extends CloseableIterable[Barcodes]:

  // used to attempt to parse barcodes out of ids if the file has no associated barcode
  private val colBarcodeParser = Dmuxed.barcodeFromId(colBarcodeLength)

  private def colBarcodeOpt = parser.indexBarcode

  private[this] class BarcodeIterator(iterator: CloseableIterator[Read]) extends CloseableIterator[Barcodes]:
    override def hasNext: Boolean = iterator.hasNext

    override def next(): Barcodes =
      val nextRead = iterator.next()
      val rowBarcodeOpt = rowPolicy.find(nextRead)
      val umiBarcodeOpt = umiPolicyOpt.flatMap(_.find(nextRead))
      Barcodes(rowBarcodeOpt, None, colBarcodeOpt.orElse(colBarcodeParser(nextRead.id)), umiBarcodeOpt)

    override def close(): Unit = iterator.close()

  end BarcodeIterator

  override def iterator: CloseableIterator[Barcodes] = new BarcodeIterator(parser.iterator)

end DmuxedBarcodeSource
