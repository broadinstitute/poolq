/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import cats.syntax.all.*
import munit.FunSuite

class BarcodePolicyTest extends FunSuite:

  test("fixed barcode policy") {
    assertEquals(BarcodePolicy("FIXED@0", 8.some, false), FixedOffsetPolicy(0, 8, false))
    // `:` is a deprecated option but needs to be supported for the time being
    assertEquals(BarcodePolicy("FIXED:0", 8.some, false), FixedOffsetPolicy(0, 8, false))

    // a length may be provided as part of the policy
    assertEquals(BarcodePolicy("FIXED@0:6", None, true), FixedOffsetPolicy(0, 6, true))

    // if a length is not part of the policy string, it must be provided
    intercept[IllegalArgumentException](BarcodePolicy("FIXED@0", None, false))
  }

  test("known prefix barcode policy") {
    assertEquals(BarcodePolicy("PREFIX:CACCG@7", 20.some, false), IndexOfKnownPrefixPolicy("CACCG", 20, Some(7)))
    assertEquals(
      BarcodePolicy("PREFIX:CACCG@7-9", 20.some, false),
      IndexOfKnownPrefixPolicy("CACCG", 20, Some(7), Some(9))
    )
    assertEquals(BarcodePolicy("PREFIX:CACCG@-9", 20.some, false), IndexOfKnownPrefixPolicy("CACCG", 20, None, Some(9)))

    // a length may be provided as part of the policy
    assertEquals(BarcodePolicy("PREFIX:CACCG@7:20", None, false), IndexOfKnownPrefixPolicy("CACCG", 20, Some(7)))
  }

  test("specify a shorter length with a fixed policy") {
    assertEquals(BarcodePolicy("FIXED@0:6", 8.some, true), FixedOffsetPolicy(0, 6, true))
    // `:` is a deprecated option but it needs to be supported for the time being
    assertEquals(BarcodePolicy("FIXED:0:6", 6.some, true), FixedOffsetPolicy(0, 6, true))
  }

  test("specify a shorter length with a known prefix policy") {
    assertEquals(BarcodePolicy("PREFIX:CACCG@7:19", 20.some, false), IndexOfKnownPrefixPolicy("CACCG", 19, Some(7)))
  }

  test("keymask policy") {
    assertEquals(
      BarcodePolicy("KEYMASK:caccgNNNNttNNNNaa@3", 8.some, false),
      GeneralTemplatePolicy(KeyMask("caccgNNNNttNNNNaa"), Some(3), None)
    )
    assertEquals(
      BarcodePolicy("TEMPLATE:caccgNNNNttNNNNaa@3", 8.some, false),
      GeneralTemplatePolicy(KeyMask("caccgNNNNttNNNNaa"), Some(3), None)
    )
  }

  test("split barcode situation") {
    assertEquals(
      BarcodePolicy("TEMPLATE:caccgNNNNNnnnnnntatgcNNNNaa@3", 9.some, false),
      SplitBarcodePolicy("CACCG", 5, 6, "TATGC", 4, Some(3), None)
    )
    // the provided reference file must match
    intercept[IllegalArgumentException](BarcodePolicy("TEMPLATE:caccgNNNNNnnnnnntatgcNNNNaa@3", 10.some, false))
  }

  test("specify just a 3' limit") {
    assertEquals(
      BarcodePolicy("TEMPLATE:NNNNNNNNNNNNNNNNNNNNNNN@-1", 23.some, false),
      GeneralTemplatePolicy(KeyMask("NNNNNNNNNNNNNNNNNNNNNNN"), None, Some(1))
    )
  }

end BarcodePolicyTest
