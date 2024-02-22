/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import munit.FunSuite

class BarcodePolicyTest extends FunSuite:

  test("fixed barcode policy") {
    assertEquals(BarcodePolicy("FIXED@0", 8, false), FixedOffsetPolicy(0, 8, false))
    // this is a deprecated option but needs to be supported for the time being
    assertEquals(BarcodePolicy("FIXED:0", 8, false), FixedOffsetPolicy(0, 8, false))
  }

  test("known prefix barcode policy") {
    assertEquals(BarcodePolicy("PREFIX:CACCG@7", 20, false), IndexOfKnownPrefixPolicy("CACCG", 20, Some(7)))
    assertEquals(BarcodePolicy("PREFIX:CACCG@7-9", 20, false), IndexOfKnownPrefixPolicy("CACCG", 20, Some(7), Some(9)))
    assertEquals(BarcodePolicy("PREFIX:CACCG@-9", 20, false), IndexOfKnownPrefixPolicy("CACCG", 20, None, Some(9)))
  }

  test("specify a shorter length with a fixed policy") {
    assertEquals(BarcodePolicy("FIXED@0:6", 6, true), FixedOffsetPolicy(0, 6, true))
    // this is a deprecated option but needs to be supported for the time being
    assertEquals(BarcodePolicy("FIXED:0:6", 6, true), FixedOffsetPolicy(0, 6, true))
  }

  test("specify a shorter length with a known prefix policy") {
    assertEquals(BarcodePolicy("PREFIX:CACCG@7:19", 19, false), IndexOfKnownPrefixPolicy("CACCG", 19, Some(7)))
  }

  test("keymask policy") {
    assertEquals(
      BarcodePolicy("KEYMASK:caccgNNNNttNNNNaa@3", 8, false),
      GeneralTemplatePolicy(KeyMask("caccgNNNNttNNNNaa"), Some(3), None)
    )
    assertEquals(
      BarcodePolicy("TEMPLATE:caccgNNNNttNNNNaa@3", 8, false),
      GeneralTemplatePolicy(KeyMask("caccgNNNNttNNNNaa"), Some(3), None)
    )
  }

  test("split barcode situation") {
    assertEquals(
      BarcodePolicy("TEMPLATE:caccgNNNNNnnnnnntatgcNNNNaa@3", 9, false),
      SplitBarcodePolicy("CACCG", 5, 6, "TATGC", 4, Some(3), None)
    )
  }

  test("specify just a 3' limit") {
    assertEquals(
      BarcodePolicy("TEMPLATE:NNNNNNNNNNNNNNNNNNNNNNN@-1", 23, false),
      GeneralTemplatePolicy(KeyMask("NNNNNNNNNNNNNNNNNNNNNNN"), None, Some(1))
    )
  }

end BarcodePolicyTest
