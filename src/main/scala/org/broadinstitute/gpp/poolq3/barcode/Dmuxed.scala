/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

object Dmuxed:

  private[barcode] def barcodeFromId(length: Int): String => Option[FoundBarcode] =
    val regex = s"@.*[^ACGTN]([ACGTN]{$length})$$".r
    _ match
      case regex(barcode) => Some(FoundBarcode(barcode.toCharArray, 0))
      case _              => None
