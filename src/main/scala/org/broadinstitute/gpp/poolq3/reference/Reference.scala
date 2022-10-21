/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reference

import scala.collection.mutable

import it.unimi.dsi.fastutil.objects.{Object2ObjectMap, Object2ObjectOpenHashMap}
import org.broadinstitute.gpp.poolq3.parser.{ConflictingBarcodeException, ReferenceEntry}

/** Represents one dimension of reference data, which is a mapping between DNA barcodes and associated identifiers.
  */
trait Reference {

  /** Finds the best matching barcode in the reference data for the barcode provided */
  def find(barcode: String): Seq[MatchedBarcode]

  /** Finds the best matching barcode in the reference data for the barcode provided */
  def find(barcode: Array[Char]): Seq[MatchedBarcode]

  /** Returns true iff the provided barcode has a match in the reference data */
  def isDefined(barcode: String): Boolean

  /** Returns the set of IDs matching the provided barcode, if any */
  def idsForBarcode(barcode: String): Seq[String]

  /** Returns the barcodes that map to the provided ID, if any */
  def barcodesForId(id: String): Seq[String]

  /** Returns all barcodes in the reference data, in the order originally provided */
  def allBarcodes: Seq[String]

  /** Returns the corresponding reference barcode for the DNA barcode as found in the reads */
  def referenceBarcodeForDnaBarcode(dnaBarcode: String): String

  /** Returns all barcode IDs in the reference data, in the order originally provided */
  def allIds: Seq[String]

  /** Returns the length of the barcodes represented by this reference */
  def barcodeLength: Int

}

object Reference {

  type Mappings =
    (Seq[String], Object2ObjectMap[String, String], Object2ObjectMap[String, mutable.LinkedHashSet[String]])

  def build(mappings: Seq[ReferenceEntry]): Mappings = {
    // these make up the return type
    val barcodes = new mutable.LinkedHashSet[String]
    val barcodeIds = new Object2ObjectOpenHashMap[String, mutable.LinkedHashSet[String]]()

    // this is used transiently for verification
    val barcodeToInputBarcode = new Object2ObjectOpenHashMap[String, String]()

    mappings.foreach { referenceEntry =>
      barcodes += referenceEntry.dnaBarcode
      // check that for any barcode used in matching, only one _input_ barcode reduces to it
      if (barcodeToInputBarcode.containsKey(referenceEntry.dnaBarcode)) {
        val witness = barcodeToInputBarcode.get(referenceEntry.dnaBarcode)
        if (witness != referenceEntry.referenceBarcode) {
          throw new ConflictingBarcodeException(referenceEntry.referenceBarcode, referenceEntry.referenceId, witness)
        }
      } else {
        barcodeToInputBarcode.put(referenceEntry.dnaBarcode, referenceEntry.referenceBarcode)
      }

      // this looks inefficient, but using `.putIfAbsent` appears to basically do the same thing,
      // with the exception that `.putIfAbsent` requires a new set to be constructed every time
      // regardless of whether it's used, since it's a Java API and Java doesn't support by-name
      // parameters
      if (barcodeIds.containsKey(referenceEntry.dnaBarcode)) {
        barcodeIds.get(referenceEntry.dnaBarcode) += referenceEntry.referenceId
      } else {
        val set = mutable.LinkedHashSet[String]()
        set += referenceEntry.referenceId
        barcodeIds.put(referenceEntry.dnaBarcode, set)
      }
    }

    (barcodes.toVector, barcodeToInputBarcode, barcodeIds)
  }

  def truncationVariants(
    barcodes: Seq[String],
    barcodeProcessor: String => String,
    includeAmbiguous: Boolean
  ): Object2ObjectMap[String, List[String]] = {
    val map = new Object2ObjectOpenHashMap[String, List[String]]
    map.defaultReturnValue(Nil)
    barcodes.foreach { barcode =>
      val variant = barcodeProcessor(barcode)
      val bcs = map.get(variant)
      map.put(variant, barcode :: bcs)
    }

    // truncation is one of 2 ways we can end up with ambiguous matches
    if (!includeAmbiguous) {
      Reference.pruneAmbiguous(map)
    }

    map
  }

  def pruneAmbiguous[S <: Seq[String]](barcodeVariants: Object2ObjectMap[String, S]): Unit = {
    // list ambiguous variants
    var ambiguousVariants: List[String] = Nil
    barcodeVariants.forEach { (variant, barcodes) =>
      if (barcodes.size > 1) {
        ambiguousVariants ::= variant
      }
    }
    // remove them from `barcodeVariants`
    ambiguousVariants.foreach(barcodeVariants.remove)
  }

}
