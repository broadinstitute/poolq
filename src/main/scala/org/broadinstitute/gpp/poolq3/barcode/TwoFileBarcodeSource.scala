/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, CloseableIterator}
import org.broadinstitute.gpp.poolq3.types.{Read, ReadIdCheckPolicy}

final class TwoFileBarcodeSource(
    rowParser: CloseableIterable[Read],
    colParser: CloseableIterable[Read],
    rowPolicy: BarcodePolicy,
    columnPolicy: BarcodePolicy,
    umiPolicyOpt: Option[BarcodePolicy],
    readIdCheckPolicy: ReadIdCheckPolicy
) extends CloseableIterable[Barcodes]:

  private class BarcodeIterator(rowIterator: CloseableIterator[Read], colIterator: CloseableIterator[Read])
      extends CloseableIterator[Barcodes]:

    final override def hasNext: Boolean = rowIterator.hasNext && colIterator.hasNext

    final override def next(): Barcodes =
      val nextRow = rowIterator.next()
      val nextCol = colIterator.next()
      readIdCheckPolicy.check(nextRow, nextCol)
      val rowBarcodeOpt = rowPolicy.find(nextRow)
      val colBarcodeOpt = columnPolicy.find(nextCol)
      val umiBarcodeOpt = umiPolicyOpt.flatMap(_.find(nextRow))
      Barcodes(rowBarcodeOpt, None, colBarcodeOpt, umiBarcodeOpt)

    end next

    final override def close(): Unit =
      try rowIterator.close()
      finally colIterator.close()

  end BarcodeIterator

  override def iterator: CloseableIterator[Barcodes] =
    new BarcodeIterator(rowParser.iterator, colParser.iterator)

end TwoFileBarcodeSource
