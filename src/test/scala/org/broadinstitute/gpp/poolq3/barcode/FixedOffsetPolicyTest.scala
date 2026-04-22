/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import munit.{FunSuite, ScalaCheckSuite}
import org.broadinstitute.gpp.poolq3.gen.{acgtn, dnaSeq}
import org.broadinstitute.gpp.poolq3.types.Read
import org.scalacheck.Prop
import org.scalacheck.Prop.forAll

class FixedOffsetPolicyTest extends FunSuite with ScalaCheckSuite:

  test("find should find the barcode in the read") {
    forAll(dnaSeq(acgtn), dnaSeq(acgtn), dnaSeq(acgtn)) { (a: String, b: String, c: String) =>
      val read = Read("id", a + b + c)
      val policy = FixedOffsetPolicy(a.length, b.length, skipShortReads = false)
      assertEquals(policy.find(read), Some(FoundBarcode(b.toCharArray, a.length)))
    }
  }

  test("should skip short reads when asked") {
    forAll(dnaSeq(acgtn), dnaSeq(acgtn)) { (a: String, b: String) =>
      val read = Read("id", a + b)
      val policy = FixedOffsetPolicy(a.length + 1, b.length, skipShortReads = true)
      assertEquals(policy.find(read), None)
    }
  }

  test("should reject short reads") {
    forAll(dnaSeq(acgtn), dnaSeq(acgtn)) { (a: String, b: String) =>
      val read = Read("id", a + b)
      val policy = FixedOffsetPolicy(a.length + 1, b.length, skipShortReads = false)
      Prop.secure {
        val _ = intercept[ReadTooShortException] {
          policy.find(read)
        }
        true
      }
    }
  }

end FixedOffsetPolicyTest
