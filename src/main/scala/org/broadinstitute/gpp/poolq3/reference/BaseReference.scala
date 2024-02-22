/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reference

import scala.collection.mutable

import it.unimi.dsi.fastutil.objects.Object2ObjectMap

abstract class BaseReference(
  final val allBarcodes: Seq[String],
  barcodeToInputBarcode: Object2ObjectMap[String, String],
  barcodeEntries: Object2ObjectMap[String, mutable.LinkedHashSet[String]]
) extends Reference:

  require(allBarcodes.nonEmpty, "Reference may not be empty")

  final def find(barcode: Array[Char]): Seq[MatchedBarcode] =
    find(new String(barcode))

  final def isDefined(barcode: String): Boolean = find(barcode).nonEmpty

  final def idsForBarcode(barcode: String): Seq[String] =
    val ids = barcodeEntries.get(barcode)
    if ids == null then Vector.empty
    else ids.toVector

  final def barcodesForId(id: String): Seq[String] =
    barcodesForIdMap.getOrElse(id, Nil)

  /** Creates a map between IDs and the list of associated barcodes. For row barcodes, we expect each ID to be
    * associated with precisely one barcode, thus the lists will be singletons. For column barcodes, however, it is
    * common for a single ID to be represented by several barcodes (indicating replicates). Thus, the value lists may
    * contain several distinct barcodes.
    */
  final lazy val barcodesForIdMap: Map[String, List[String]] =
    val m = mutable.HashMap[String, List[String]]()
    barcodeEntries.forEach((barcode, ids) => ids.foreach(id => m.put(id, barcode :: m.getOrElse(id, Nil))))
    m.toMap

  def referenceBarcodeForDnaBarcode(matchingBarcode: String): String =
    val inputBarcode = barcodeToInputBarcode.get(matchingBarcode)
    if inputBarcode == null then throw new IllegalArgumentException(s"Unknown matching barcode $matchingBarcode")
    else inputBarcode

  final lazy val allIds: Seq[String] =
    val ids = mutable.LinkedHashSet[String]()
    allBarcodes.foreach(barcode => ids ++= barcodeEntries.get(barcode))
    ids.toVector

  final val barcodeLength: Int = allBarcodes.head.length

end BaseReference
