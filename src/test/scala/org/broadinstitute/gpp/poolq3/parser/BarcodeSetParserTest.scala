/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import scala.util.Success

import munit.{FunSuite, ScalaCheckSuite}
import org.broadinstitute.gpp.poolq3.TestResources
import org.broadinstitute.gpp.poolq3.gen.{acgt, nonEmptyDnaSeq}
import org.scalacheck.Prop.forAll

class BarcodeSetParserTest extends FunSuite with ScalaCheckSuite with TestResources {

  property("parseBarcode") {
    forAll(nonEmptyDnaSeq(acgt)) { bc =>
      assertEquals(BarcodeSet.parseBarcode(new IllegalArgumentException(_))(bc), Success(bc))
    }
  }

  test("BarcodeSet") {
    val bs = BarcodeSet(resourcePath("umi.txt"))
    assertEquals(bs.barcodeLength, 5)
    assertEquals(bs.barcodes.size, 14)
  }

  test("BarcodeSet failure") {
    intercept[InvalidFileException](BarcodeSet(resourcePath("bad-umi.txt")))
  }

}
