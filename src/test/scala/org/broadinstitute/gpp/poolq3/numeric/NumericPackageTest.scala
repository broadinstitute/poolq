/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.numeric

import munit.{FunSuite, ScalaCheckSuite}
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class NumericPackageTest extends FunSuite with ScalaCheckSuite:

  test("log2 should take the log base 2") {
    val smallNonNeg = Gen.chooseNum(0.0, 48.0)
    forAll(smallNonNeg) { (x: Double) =>
      val expected = x
      val actual = log2(math.pow(2, x))
      assert(math.abs(actual - expected) <= .00000000000001)
    }
  }

  test("logNormalize should not divide by zero") {
    assertEquals(logNormalize(0, 132656131), 0.0)
    assertEquals(logNormalize(0, 0), 0.0)
    assertEquals(logNormalize(1, 0), 0.0)
  }

  test("should compute values") {
    forAll(Gen.posNum[Int], Gen.posNum[Int])((x: Int, y: Int) => assert(logNormalize(x, y) >= 0.0))
  }

end NumericPackageTest
