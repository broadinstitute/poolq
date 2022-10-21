/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import scala.util.Using

import munit.FunSuite
import org.broadinstitute.gpp.poolq3.TestResources

class SamParserTest extends FunSuite with TestResources {

  test("SamParser") {
    val file = resourcePath("sample.bam")
    val parser = new SamParser(file)
    Using.resource(parser.iterator) { iter =>
      val reads = iter.toList
      assertEquals(reads.length, 4)
    }
  }

}
