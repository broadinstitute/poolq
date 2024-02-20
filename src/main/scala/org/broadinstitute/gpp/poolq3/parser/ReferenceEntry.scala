/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

case class ReferenceEntry(referenceBarcode: String, referenceId: String):
  val dnaBarcode: String = referenceBarcode.replaceAll("[:;-]", "").toUpperCase
  def barcodeLength: Int = dnaBarcode.length

  def barcodeLengths: (Int, Int) =
    val split = referenceBarcode.indexWhere(Set(';', ':', '-'))
    if split > 0 then (split, barcodeLength - split)
    else (barcodeLength, 0)

end ReferenceEntry
