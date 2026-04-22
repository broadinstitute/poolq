/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reference

import scala.util.Random

import munit.{FunSuite, ScalaCheckSuite}
import org.broadinstitute.gpp.poolq3.gen.barcode
import org.broadinstitute.gpp.poolq3.parser.ReferenceEntry
import org.broadinstitute.gpp.poolq3.tools.withNs
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class VariantReferenceTest extends FunSuite with ScalaCheckSuite:

  private val referenceGen: Gen[List[String]] = Gen.listOfN(1000, barcode)

  test("VariantReference should find matches for a given barcode") {
    forAll(referenceGen) { (barcodes: List[String]) =>
      val reference = VariantReference(barcodes.map(b => ReferenceEntry(b, b)), identity, false)

      barcodes.foreach(barcode => assertEquals(reference.find(barcode), Seq(MatchedBarcode(barcode, 0))))
    }
  }

  test("should find matches for barcodes with zero or one Ns") {
    forAll(referenceGen) { (barcodes: List[String]) =>
      val reference = VariantReference(barcodes.map(b => ReferenceEntry(b, b)), identity, false)

      barcodes.foreach { barcode =>
        val bcN = withNs(barcode, 1)
        val bcNs = withNs(barcode, 2 + Random.nextInt(3))
        assertEquals(reference.find(barcode), Seq(MatchedBarcode(barcode, 0)))
        assertEquals(reference.find(bcN), Seq(MatchedBarcode(barcode, 1)))
        assertEquals(reference.find(bcNs), Seq())
      }
    }
  }

  test("should find variants with the correct distance") {
    val reference =
      VariantReference(Seq(ReferenceEntry("AAAAAAAAAAAAAAAAAAAA", "One")), identity, includeAmbiguous = false)
    assertEquals(reference.find("AAAAAAAAAAAAAAAAAAAA"), Seq(MatchedBarcode("AAAAAAAAAAAAAAAAAAAA", 0)))
    assertEquals(reference.find("NAAAAAAAAAAAAAAAAAAA"), Seq(MatchedBarcode("AAAAAAAAAAAAAAAAAAAA", 1)))
    assertEquals(reference.find("AAAAAAAAAAAAAAAAAAAT"), Seq(MatchedBarcode("AAAAAAAAAAAAAAAAAAAA", 1)))
    assertEquals(reference.find("NAAAAAAAAAAAAAAAAAAT"), Seq())
  }

  test("should handle truncated barcodes without Ns") {
    implicit val ord: Ordering[MatchedBarcode] = Ordering.by(mb => (mb.barcode, mb.distance))

    val barcodes = Seq(ReferenceEntry("AAAAAAAAAAAAAAAAAAAA", "One"), ReferenceEntry("AAAAAAAAAAAAAAAAAAAT", "Two"))

    val reference1 = VariantReference(barcodes, _.dropRight(1), includeAmbiguous = false)
    assertEquals(reference1.find("AAAAAAAAAAAAAAAAAAA"), Seq())

    val reference2 = VariantReference(barcodes, _.dropRight(1), includeAmbiguous = true)
    assertEquals(
      reference2.find("AAAAAAAAAAAAAAAAAAA").sorted,
      barcodes.map(bc => MatchedBarcode(bc.dnaBarcode, 0)).sorted
    )
  }

  test("should handle truncated barcodes with Ns") {
    implicit val ord: Ordering[MatchedBarcode] = Ordering.by(mb => (mb.barcode, mb.distance))

    val barcodes = Seq(ReferenceEntry("AAAAAAAAAAAAAAAAAAAA", "One"), ReferenceEntry("AAAAAAAAAAAAAAAAAATA", "Two"))

    val reference1 = VariantReference(barcodes, _.dropRight(1), includeAmbiguous = false)
    assertEquals(reference1.find("AAAAAAAAAAAAAAAAAAN"), Seq())

    val reference2 = VariantReference(barcodes, _.dropRight(1), includeAmbiguous = true)
    assertEquals(
      reference2.find("AAAAAAAAAAAAAAAAAAN").sorted,
      barcodes.map(bc => MatchedBarcode(bc.dnaBarcode, 1)).sorted
    )
  }

end VariantReferenceTest
