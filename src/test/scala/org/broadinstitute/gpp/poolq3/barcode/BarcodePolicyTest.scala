/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class BarcodePolicyTest extends AnyFlatSpec {

  "BarcodePolicy" should "choose a fixed barcode policy" in {
    BarcodePolicy("FIXED@0", 8, false) should be(FixedOffsetPolicy(0, 8, false))
    // this is a deprecated option but needs to be supported for the time being
    BarcodePolicy("FIXED:0", 8, false) should be(FixedOffsetPolicy(0, 8, false))
  }

  it should "choose a known prefix barcode policy" in {
    BarcodePolicy("PREFIX:CACCG@7", 20, false) should be(IndexOfKnownPrefixPolicy("CACCG", 20, Some(7)))
    BarcodePolicy("PREFIX:CACCG@7-9", 20, false) should be(IndexOfKnownPrefixPolicy("CACCG", 20, Some(7), Some(9)))
    BarcodePolicy("PREFIX:CACCG@-9", 20, false) should be(IndexOfKnownPrefixPolicy("CACCG", 20, None, Some(9)))
  }

  it should "let the user specify a shorter length with a fixed policy" in {
    BarcodePolicy("FIXED@0:6", 6, true) should be(FixedOffsetPolicy(0, 6, true))
    // this is a deprecated option but needs to be supported for the time being
    BarcodePolicy("FIXED:0:6", 6, true) should be(FixedOffsetPolicy(0, 6, true))
  }

  it should "let the user specify a shorter length with a known prefix policy" in {
    BarcodePolicy("PREFIX:CACCG@7:19", 19, false) should be(IndexOfKnownPrefixPolicy("CACCG", 19, Some(7)))
  }

  it should "let the user specify a keymask policy" in {
    BarcodePolicy("KEYMASK:caccgNNNNttNNNNaa@3", 8, false) should be(
      TemplatePolicy(KeyMask("caccgNNNNttNNNNaa"), Some(3), None)
    )
    BarcodePolicy("TEMPLATE:caccgNNNNttNNNNaa@3", 8, false) should be(
      TemplatePolicy(KeyMask("caccgNNNNttNNNNaa"), Some(3), None)
    )
  }

  it should "let the user specify just a 3' limit" in {
    BarcodePolicy("TEMPLATE:NNNNNNNNNNNNNNNNNNNNNNN@-1", 23, false) should be(
      TemplatePolicy(KeyMask("NNNNNNNNNNNNNNNNNNNNNNN"), None, Some(1))
    )
  }

}
