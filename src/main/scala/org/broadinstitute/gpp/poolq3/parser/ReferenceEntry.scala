/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import scala.annotation.tailrec

final case class ReferenceEntry(referenceBarcode: String, referenceId: String):
  val dnaBarcode: String = referenceBarcode.replaceAll("[:;-]", "").toUpperCase
  def barcodeLength: Int = dnaBarcode.length

  def containsMultiple(s: String, p: Char => Boolean): Boolean =
    @tailrec def loop(i: Int, alreadyFound: Boolean): Boolean =
      if i > s.length - 1 then false
      // todo: rephrase this so it's not incomprehensible
      else if p(s.charAt(i)) then if alreadyFound then true else loop(i + 1, true)
      else loop(i + 1, alreadyFound)
    loop(0, false)

  def barcodeLengths: Option[(Int, Int)] =
    if containsMultiple(referenceBarcode, ReferenceEntity.Delimiters) then None
    else
      val split = referenceBarcode.indexWhere(Set(';', ':', '-'))
      if split > 0 then Some((split, barcodeLength - split))
      else None

end ReferenceEntry

object ReferenceEntity:

  val Delimiters: Set[Char] = Set(';', ':', '-')

end ReferenceEntity
