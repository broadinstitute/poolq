/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reference

import scala.util.Random

import org.broadinstitute.gpp.poolq3.gen.barcode
import org.broadinstitute.gpp.poolq3.parser.ReferenceEntry
import org.broadinstitute.gpp.poolq3.tools.withNs
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks._

class VariantReferenceTest extends AnyFlatSpec {

  private[this] val referenceGen: Gen[List[String]] = Gen.listOfN(1000, barcode)

  "VariantReference" should "find matches for a given barcode" in {
    forAll(referenceGen) { (barcodes: List[String]) =>
      val reference = VariantReference(barcodes.map(b => ReferenceEntry(b, b)), identity, false)

      barcodes.foreach(barcode => reference.find(barcode) should be(Seq(MatchedBarcode(barcode, 0))))
    }
  }

  it should "find matches for barcodes with zero or one Ns" in {
    forAll(referenceGen) { (barcodes: List[String]) =>
      val reference = VariantReference(barcodes.map(b => ReferenceEntry(b, b)), identity, false)

      barcodes.foreach { barcode =>
        val bcN = withNs(barcode, 1)
        val bcNs = withNs(barcode, 2 + Random.nextInt(3))
        reference.find(barcode) should be(Seq(MatchedBarcode(barcode, 0)))
        reference.find(bcN) should be(Seq(MatchedBarcode(barcode, 1)))
        reference.find(bcNs) should be(Seq())
      }
    }
  }

  it should "find variants with the correct distance" in {
    val reference =
      VariantReference(Seq(ReferenceEntry("AAAAAAAAAAAAAAAAAAAA", "One")), identity, includeAmbiguous = false)
    reference.find("AAAAAAAAAAAAAAAAAAAA") should be(Seq(MatchedBarcode("AAAAAAAAAAAAAAAAAAAA", 0)))
    reference.find("NAAAAAAAAAAAAAAAAAAA") should be(Seq(MatchedBarcode("AAAAAAAAAAAAAAAAAAAA", 1)))
    reference.find("AAAAAAAAAAAAAAAAAAAT") should be(Seq(MatchedBarcode("AAAAAAAAAAAAAAAAAAAA", 1)))
    reference.find("NAAAAAAAAAAAAAAAAAAT") should be(Seq())
  }

  it should "handle truncated barcodes without Ns" in {
    implicit val ord: Ordering[MatchedBarcode] = Ordering.by(mb => (mb.barcode, mb.distance))

    val barcodes = Seq(ReferenceEntry("AAAAAAAAAAAAAAAAAAAA", "One"), ReferenceEntry("AAAAAAAAAAAAAAAAAAAT", "Two"))

    val reference1 = VariantReference(barcodes, _.dropRight(1), includeAmbiguous = false)
    reference1.find("AAAAAAAAAAAAAAAAAAA") should be(Seq())

    val reference2 = VariantReference(barcodes, _.dropRight(1), includeAmbiguous = true)
    reference2.find("AAAAAAAAAAAAAAAAAAA").sorted should be(barcodes.map(bc => MatchedBarcode(bc.dnaBarcode, 0)).sorted)
  }

  it should "handle truncated barcodes with Ns" in {
    implicit val ord: Ordering[MatchedBarcode] = Ordering.by(mb => (mb.barcode, mb.distance))

    val barcodes = Seq(ReferenceEntry("AAAAAAAAAAAAAAAAAAAA", "One"), ReferenceEntry("AAAAAAAAAAAAAAAAAATA", "Two"))

    val reference1 = VariantReference(barcodes, _.dropRight(1), includeAmbiguous = false)
    reference1.find("AAAAAAAAAAAAAAAAAAN") should be(Seq())

    val reference2 = VariantReference(barcodes, _.dropRight(1), includeAmbiguous = true)
    reference2.find("AAAAAAAAAAAAAAAAAAN").sorted should be(barcodes.map(bc => MatchedBarcode(bc.dnaBarcode, 1)).sorted)
  }

}
