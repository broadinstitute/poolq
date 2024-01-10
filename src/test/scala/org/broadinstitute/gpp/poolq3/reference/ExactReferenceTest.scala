/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reference

import org.broadinstitute.gpp.poolq3.gen.barcode
import org.broadinstitute.gpp.poolq3.parser.ReferenceEntry
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks.*

class ExactReferenceTest extends AnyFlatSpec {
  val referenceGen: Gen[List[String]] = Gen.listOfN(1000, barcode)

  "ExactReference" should "find matches for a given barcode" in {
    forAll(referenceGen) { (barcodes: List[String]) =>
      val reference = ExactReference(barcodes.map(b => ReferenceEntry(b, b)), identity, includeAmbiguous = false)

      barcodes.foreach { bc =>
        val _ = reference.isDefined(bc) should be(true)
        val _ = reference.find(bc) should be(Seq(MatchedBarcode(bc, 0)))
        reference.idsForBarcode(bc) should be(Seq(bc))
      }

      val _ = reference.allIds.toSet should be(barcodes.toSet)
      reference.allBarcodes.toSet should be(barcodes.toSet)
    }
  }

  it should "find variants with the correct distance" in {
    val reference =
      ExactReference(Seq(ReferenceEntry("AAAAAAAAAAAAAAAAAAAA", "One")), identity, includeAmbiguous = false)
    val _ = reference.find("AAAAAAAAAAAAAAAAAAAA") should be(Seq(MatchedBarcode("AAAAAAAAAAAAAAAAAAAA", 0)))
    val _ = reference.find("NAAAAAAAAAAAAAAAAAAA") should be(Seq())
    val _ = reference.find("AAAAAAAAAAAAAAAAAAAT") should be(Seq())
    val _ = reference.find("NAAAAAAAAAAAAAAAAAAT") should be(Seq())

    val _ = reference.barcodesForId("One") should be(Seq("AAAAAAAAAAAAAAAAAAAA"))
    reference.idsForBarcode("AAAAAAAAAAAAAAAAAAAA") should be(Seq("One"))
  }

}
