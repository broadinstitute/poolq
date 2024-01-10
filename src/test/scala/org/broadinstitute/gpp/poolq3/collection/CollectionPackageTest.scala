/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.collection

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*

class CollectionPackageTest extends AnyFlatSpec {

  "zipWithIndex1" should "produce indexes starting with 1" in {
    val input = Seq("a", "b", "c")
    input.iterator.zipWithIndex1.toSeq should be(Seq(("a", 1), ("b", 2), ("c", 3)))
  }

}
