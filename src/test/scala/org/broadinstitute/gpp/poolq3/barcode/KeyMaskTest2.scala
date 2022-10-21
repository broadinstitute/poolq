/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

/** This class provides additional tests for the KeyMask that are not found in the FISHR codebase from which KeyMask and
  * its primary test class were lifted. We will try not to modify the copied test classes to make subsequent updates
  * from FISHR easier. Instead, new PoolQ-specific tests will live here.
  */
class KeyMaskTest2 extends AnyFlatSpec {

  "KeyMask.apply" should "construct the correct key mask from a pattern" in {
    //                 0        1         2
    //                 12345678901234567890123456789
    val km0 = KeyMask("caccgNNNNNnnnnnnnnnttacaNNNNN")

    // parsing should work how we expect
    km0.keyRanges should be(Seq(KeyRange(5, 9), KeyRange(24, 28)))
    km0.keyLengthInBases should be(10)
  }

}
