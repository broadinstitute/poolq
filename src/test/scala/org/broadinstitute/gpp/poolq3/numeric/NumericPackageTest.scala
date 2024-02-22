/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.numeric

import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks.*

class NumericPackageTest extends AnyFlatSpec:

  "log2" should "take the log base 2" in {
    val smallNonNeg = Gen.chooseNum(0.0, 48.0)
    forAll(smallNonNeg)((x: Double) => log2(math.pow(2, x)) should be(x +- .00000000000001))
  }

  "logNormalize" should "not divide by zero" in {
    val _ = logNormalize(0, 132656131) should be(0)
    val _ = logNormalize(0, 0) should be(0)
    logNormalize(1, 0) should be(0)
  }

  it should "compute values" in {
    forAll(Gen.posNum[Int], Gen.posNum[Int]) { (x: Int, y: Int) =>
      implicit val ord: Ordering[Double] = Ordering.Double.IeeeOrdering
      logNormalize(x, y) should be >= 0.0
    }
  }

end NumericPackageTest
