/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reference

import munit.{FunSuite, ScalaCheckSuite}
import org.broadinstitute.gpp.poolq3.gen.barcode
import org.broadinstitute.gpp.poolq3.parser.ReferenceEntry
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class ExactReferenceTest extends FunSuite with ScalaCheckSuite:
  val referenceGen: Gen[List[String]] = Gen.listOfN(1000, barcode)

  test("ExactReference should find matches for a given barcode") {
    forAll(referenceGen) { (barcodes: List[String]) =>
      val reference = ExactReference(barcodes.map(b => ReferenceEntry(b, b)), identity, includeAmbiguous = false)

      barcodes.foreach { bc =>
        assert(reference.isDefined(bc))
        assertEquals(reference.find(bc), Seq(MatchedBarcode(bc, 0)))
        assertEquals(reference.idsForBarcode(bc), Seq(bc))
      }

      assertEquals(reference.allIds.toSet, barcodes.toSet)
      assertEquals(reference.allBarcodes.toSet, barcodes.toSet)
    }
  }

  test("should find variants with the correct distance") {
    val reference =
      ExactReference(Seq(ReferenceEntry("AAAAAAAAAAAAAAAAAAAA", "One")), identity, includeAmbiguous = false)
    assertEquals(reference.find("AAAAAAAAAAAAAAAAAAAA"), Seq(MatchedBarcode("AAAAAAAAAAAAAAAAAAAA", 0)))
    assertEquals(reference.find("NAAAAAAAAAAAAAAAAAAA"), Seq())
    assertEquals(reference.find("AAAAAAAAAAAAAAAAAAAT"), Seq())
    assertEquals(reference.find("NAAAAAAAAAAAAAAAAAAT"), Seq())

    assertEquals(reference.barcodesForId("One"), Seq("AAAAAAAAAAAAAAAAAAAA"))
    assertEquals(reference.idsForBarcode("AAAAAAAAAAAAAAAAAAAA"), Seq("One"))
  }

end ExactReferenceTest
