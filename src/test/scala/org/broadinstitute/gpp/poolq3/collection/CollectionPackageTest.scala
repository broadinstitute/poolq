/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.collection

import munit.FunSuite

class CollectionPackageTest extends FunSuite:

  test("zipWithIndex1 should produce indexes starting with 1") {
    val input = Seq("a", "b", "c")
    assertEquals(input.iterator.zipWithIndex1.toSeq, Seq(("a", 1), ("b", 2), ("c", 3)))
  }
