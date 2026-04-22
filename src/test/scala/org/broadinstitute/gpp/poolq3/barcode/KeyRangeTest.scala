/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import munit.FunSuite

/** @author
  *   Broad Institute Genetic Perturbation Platform
  */
class KeyRangeTest extends FunSuite:

  test("KeyRange should enforce well-formedness") {
    val _ = KeyRange(3, 4)
    val _ = KeyRange(3, 3)
    val _ = intercept[IllegalArgumentException](KeyRange(3, 2))
    val _ = intercept[IllegalArgumentException](KeyRange(-2, 2))
  }

  test("should have working compare()") {
    val ord = implicitly[Ordering[KeyRange]]
    assertEquals(ord.compare(KeyRange(2, 5), KeyRange(2, 5)), 0)
    assert(ord.lteq(KeyRange(2, 5), KeyRange(2, 5)))
    assert(ord.gteq(KeyRange(2, 5), KeyRange(2, 5)))

    assert(ord.lt(KeyRange(2, 5), KeyRange(3, 4)))
    assert(ord.lt(KeyRange(2, 5), KeyRange(2, 6)))
    assert(ord.gt(KeyRange(2, 5), KeyRange(2, 4)))
    assert(ord.gt(KeyRange(2, 5), KeyRange(1, 32)))
  }

  test("should be creatable from a string") {
    assertEquals(KeyRange("1-1"), KeyRange(0, 0))
    assertEquals(KeyRange("1..1"), KeyRange(0, 0))
    assertEquals(KeyRange("1"), KeyRange(0, 0))
    assertEquals(KeyRange("1-6"), KeyRange(0, 5))
    assertEquals(KeyRange("1..6"), KeyRange(0, 5))
    val _ = intercept[IllegalArgumentException](KeyRange("0-5"))
    val _ = intercept[IllegalArgumentException](KeyRange("-1-5"))
    val _ = intercept[IllegalArgumentException](KeyRange("6-5"))
  }

end KeyRangeTest
