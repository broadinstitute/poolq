/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3

import java.nio.file.Paths

import munit.FunSuite
import org.broadinstitute.gpp.poolq3.parser.{ReferenceData, ReferenceEntry}

class PoolQTest extends FunSuite:

  test("makeRowBarcodePolicy works for paired end cases") {
    val rd = new ReferenceData(List(ReferenceEntry("AAAA;CCCC", "a sea")))

    val (rowPol, revPolOpt, len) =
      PoolQ.makeRowBarcodePolicy(rd, "PREFIX:CACCG@11", Some("PREFIX:CACCG@11"), Some(None -> Paths.get(".")), false)

    assertEquals(4, rowPol.length)
    assertEquals(len, 8)
    assert(revPolOpt.isDefined)
  }

  test("makeRowBarcodePolicy works for the normal case") {
    val rd = new ReferenceData(List(ReferenceEntry("AAAA;CCCC", "a sea")))

    val (rowPol, revPolOpt, len) =
      PoolQ.makeRowBarcodePolicy(rd, "PREFIX:CACCG@11", None, None, false)

    assertEquals(rowPol.length, 8)
    assertEquals(len, 8)
    assert(revPolOpt.isEmpty)
  }

end PoolQTest
