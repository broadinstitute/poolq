/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.broadinstitute.gpp.poolq3.gen.{acgtn, dnaSeq}
import org.broadinstitute.gpp.poolq3.types.Read
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks.*

class FixedOffsetPolicyTest extends AnyFlatSpec {

  "find" should "find the barcode in the read" in {
    forAll(dnaSeq(acgtn), dnaSeq(acgtn), dnaSeq(acgtn)) { (a: String, b: String, c: String) =>
      val read = Read("id", a + b + c)
      val policy = FixedOffsetPolicy(a.length, b.length, skipShortReads = false)
      policy.find(read) should contain(FoundBarcode(b.toCharArray, a.length))
    }
  }

  it should "skip short reads when asked" in {
    forAll(dnaSeq(acgtn), dnaSeq(acgtn)) { (a: String, b: String) =>
      val read = Read("id", a + b)
      val policy = FixedOffsetPolicy(a.length + 1, b.length, skipShortReads = true)
      policy.find(read) should be(None)
    }
  }

  it should "reject short reads" in {
    forAll(dnaSeq(acgtn), dnaSeq(acgtn)) { (a: String, b: String) =>
      val read = Read("id", a + b)
      val policy = FixedOffsetPolicy(a.length + 1, b.length, skipShortReads = false)
      assertThrows[ReadTooShortException](policy.find(read))
    }
  }

}
