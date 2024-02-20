/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import munit.FunSuite

class DmuxedTest extends FunSuite:

  test("extracting a barcode with Ns from an illumina read") {
    assertEquals(
      Dmuxed.barcodeFromId(8)("@A01379:680:HC37HDRX3:1:2101:1163:1000 1:N:0:GGNGNANT"),
      Some(FoundBarcode("GGNGNANT".toCharArray, 0))
    )
  }

  test("extracting a barcode with no Ns from an illumina read") {
    assertEquals(
      Dmuxed.barcodeFromId(8)("@A01379:680:HC37HDRX3:1:2101:3224:1000 1:N:0:AAATGCGA"),
      Some(FoundBarcode("AAATGCGA".toCharArray, 0))
    )
  }

  test("ignore a barcode-like sequence that's too long") {
    assertEquals(Dmuxed.barcodeFromId(8)("@A01379:680:HC37HDRX3:1:2101:3224:1000 1:N:0:AAATGCGAGG"), None)
  }

  test("ignore a barcode-like sequence that's too short") {
    assertEquals(Dmuxed.barcodeFromId(8)("@A01379:680:HC37HDRX3:1:2101:3224:1000 1:N:0:TGCGAGG"), None)
  }

end DmuxedTest
