/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reference

import scala.collection.mutable

import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import org.broadinstitute.gpp.poolq3.parser.ReferenceEntry
import org.broadinstitute.gpp.poolq3.seq.*

final class ExactReference private[ExactReference] (
  allBarcodes: Seq[String],
  barcodeToInputBarcode: Object2ObjectMap[String, String],
  barcodeIds: Object2ObjectMap[String, mutable.LinkedHashSet[String]],
  barcodeProcessor: String => String,
  includeAmbiguous: Boolean
) extends BaseReference(allBarcodes, barcodeToInputBarcode, barcodeIds):

  // we still have variants for exact matching in case we need to deal with truncated barcodes
  private[this] val truncationVariants =
    Reference.truncationVariants(allBarcodes, barcodeProcessor, includeAmbiguous)

  def find(barcode: String): Seq[MatchedBarcode] =
    if containsN(barcode) then Vector.empty[MatchedBarcode]
    else
      val barcodes: List[String] = truncationVariants.get(barcode)
      if barcodes == null then Vector.empty[MatchedBarcode]
      else barcodes.map(MatchedBarcode(_, 0))

end ExactReference

object ExactReference:

  def apply(
    mappings: Seq[ReferenceEntry],
    barcodeProcessor: String => String,
    includeAmbiguous: Boolean
  ): ExactReference =
    val (barcodes, barcodeToInputBarcode, barcodeIds) = Reference.build(mappings)
    new ExactReference(barcodes.toVector, barcodeToInputBarcode, barcodeIds, barcodeProcessor, includeAmbiguous)

end ExactReference
