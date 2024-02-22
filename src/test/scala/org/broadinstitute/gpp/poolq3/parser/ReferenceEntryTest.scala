/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import munit.FunSuite

class ReferenceEntryTest extends FunSuite:

  test("barcodeLengths detects splits") {
    val cs = "C" * 10
    val ts = "T" * 8
    val bc = s"$cs:$ts"
    val re = ReferenceEntry(bc, "Some Cs and some Ts")
    assertEquals(re.barcodeLengths, (10, 8))
  }

end ReferenceEntryTest
