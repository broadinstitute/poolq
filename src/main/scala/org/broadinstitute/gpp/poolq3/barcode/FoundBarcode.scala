/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

final case class FoundBarcode(barcode: Array[Char], offset0: Int):
  override def toString = s"FoundBarcode(${new String(barcode)}, $offset0)"

  /** This method exists purely for testing. In production, we do not compare FoundBarcode objects */
  override def equals(other: scala.Any): Boolean =
    other match
      case that: FoundBarcode =>
        (this eq that) || (this.barcode.sameElements(that.barcode) && this.offset0 == that.offset0)
      case _ => false

end FoundBarcode
