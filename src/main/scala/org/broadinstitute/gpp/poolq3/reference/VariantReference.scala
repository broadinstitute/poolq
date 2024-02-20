/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reference

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

import it.unimi.dsi.fastutil.objects.{Object2ObjectMap, Object2ObjectOpenHashMap}
import org.broadinstitute.gpp.poolq3.parser.ReferenceEntry
import org.broadinstitute.gpp.poolq3.seq.{NoN, PolyN, singleNIndex}

final class VariantReference private[VariantReference] (
  allBarcodes: Seq[String],
  barcodeToInputBarcode: Object2ObjectMap[String, String],
  barcodeIds: Object2ObjectMap[String, mutable.LinkedHashSet[String]],
  barcodeProcessor: String => String,
  includeAmbiguous: Boolean
) extends BaseReference(allBarcodes, barcodeToInputBarcode, barcodeIds):

  private[this] val truncationVariants =
    Reference.truncationVariants(allBarcodes, barcodeProcessor, includeAmbiguous)

  private[this] val mismatchVariants: Object2ObjectMap[String, List[String]] = generateVariants

  override def find(barcode: String): Seq[MatchedBarcode] =
    val bases = barcode.toCharArray
    singleNIndex(bases) match
      case NoN =>
        val exact = truncationVariants.get(barcode)
        if exact.nonEmpty then exact.map(MatchedBarcode(_, 0))
        else mismatchVariants.get(barcode).map(MatchedBarcode(_, 1))

      case PolyN => Seq.empty[MatchedBarcode]

      case n =>
        // generate all 4 possible variants at the position of the `N`
        val variants = posVariants(bases, n)

        val matchingReferenceBarcodes = variants.flatMap(truncationVariants.get(_))
        if includeAmbiguous || matchingReferenceBarcodes.lengthCompare(1) == 0 then
          // this works because the variants have the `N` replaced, so they contain only [ACGT] - that is, they must
          // match _exactly_ to a barcode in the reference file; we report an edit distance of 1 because we have to
          // assume the `N` was a mismatch
          matchingReferenceBarcodes.map(bc => MatchedBarcode(bc, 1))
        else Seq.empty

    end match

  end find

  private[this] def posVariants(bases: Array[Char], nIdx: Int, orig: Char = 'N'): Seq[String] =
    val ret = new Array[String](4)
    bases(nIdx) = 'A'
    ret(0) = new String(bases)
    bases(nIdx) = 'C'
    ret(1) = new String(bases)
    bases(nIdx) = 'G'
    ret(2) = new String(bases)
    bases(nIdx) = 'T'
    ret(3) = new String(bases)
    bases(nIdx) = orig
    ArraySeq.unsafeWrapArray(ret)

  end posVariants

  private[this] def generateVariants: Object2ObjectMap[String, List[String]] =
    val initialVariants: Seq[(String, String)] =
      allBarcodes.map(bc => (barcodeProcessor(bc), bc))

    val mismatchVariants: Seq[(String, String)] =
      for
        (variant, barcode) <- initialVariants
        bases = variant.toArray
        i <- bases.indices
        v <- posVariants(bases, i, bases(i))
      yield (v, barcode)

    // build the resulting map of variant to seq of originals
    val map = new Object2ObjectOpenHashMap[String, List[String]]
    map.defaultReturnValue(Nil)

    // populate it with both the initial variants and the mismatch variants
    mismatchVariants.foreach { case (variant, barcode) =>
      val bcs = map.get(variant)
      map.put(variant, barcode :: bcs)
    }

    if !includeAmbiguous then Reference.pruneAmbiguous(map)

    map

  end generateVariants

end VariantReference

object VariantReference:

  def apply(
    mappings: Seq[ReferenceEntry],
    barcodeProcessor: String => String,
    includeAmbiguous: Boolean
  ): VariantReference =
    val (barcodes, barcodeToInputBarcodes, barcodeIds) = Reference.build(mappings)
    new VariantReference(barcodes.toVector, barcodeToInputBarcodes, barcodeIds, barcodeProcessor, includeAmbiguous)

end VariantReference
