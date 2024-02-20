/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.{Files, Path}

import scala.util.Using

import munit.FunSuite
import org.broadinstitute.gpp.poolq3.types.Read

class TextParserTest extends FunSuite:

  test("it should parse a few sequences from a file") {
    val data =
      """AACTCACG
        |TTGAGTGC
        |TTTTTTTT""".stripMargin

    val file: Path = Files.createTempFile("TextParserTest", ".txt")
    try
      Using.resource(new BufferedWriter(new FileWriter(file.toFile)))(bw => bw.write(data))
      val fqp = new TextParser(file)
      assertEquals(fqp.toList, List(Read("Line 1", "AACTCACG"), Read("Line 2", "TTGAGTGC"), Read("Line 3", "TTTTTTTT")))
    finally
      val _ = Files.deleteIfExists(file)
  }

end TextParserTest
