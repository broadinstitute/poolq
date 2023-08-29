/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import munit.FunSuite

/** This class provides additional tests for the KeyMask that are not found in the FISHR codebase from which KeyMask and
  * its primary test class were lifted. We will try not to modify the copied test classes to make subsequent updates
  * from FISHR easier. Instead, new PoolQ-specific tests will live here.
  */
class KeyMaskTest2 extends FunSuite {

  test("construct the correct key mask from a pattern") {
    //                 0        1         2
    //                 12345678901234567890123456789
    val km0 = KeyMask("caccgNNNNNnnnnnnnnnttacaNNNNN")

    // parsing should work how we expect
    assertEquals(km0.keyRanges, Seq(KeyRange(5, 9), KeyRange(24, 28)))
    assertEquals(km0.keyLengthInBases, 10)
  }

}
