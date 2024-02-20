/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.io.{BufferedReader, StringReader}

import munit.FunSuite

class ParserPackageTest extends FunSuite:

  test("skipHeader should skip a header") {
    val source =
      s"""THIS IS A COLUMN\tTHisIS Another Column
         |AACGTGTA;TCCGTGATG\tThis is a condition
         |""".stripMargin
    val r = new BufferedReader(new StringReader(source))
    skipHeader(r, ReferenceData.LineRegex)
    assertEquals(r.readLine(), "AACGTGTA;TCCGTGATG\tThis is a condition")
  }

  test("skipHeader should not skip lines if there is no header") {
    val source =
      s"""TTTTTTTT;TTTTTTTTT\tThis is another condition
         |AACGTGTA;TCCGTGATG\tThis is a condition
         |""".stripMargin
    val r = new BufferedReader(new StringReader(source))
    skipHeader(r, ReferenceData.LineRegex)
    assertEquals(r.readLine(), "TTTTTTTT;TTTTTTTTT\tThis is another condition")
  }

  test("skipHeader should not skip lines if there is no header, quoted edition") {
    val source =
      s"""\"TTTTTTTT;TTTTTTTTT\"\t\"This is another condition\"
         |AACGTGTA;TCCGTGATG\tThis is a condition
         |""".stripMargin
    val r = new BufferedReader(new StringReader(source))
    skipHeader(r, ReferenceData.LineRegex)
    assertEquals(r.readLine(), "\"TTTTTTTT;TTTTTTTTT\"\t\"This is another condition\"")
  }

  test("skipHeader should not skip lines if there is no header, even if the ID is blank") {
    val source =
      s"""TTTTTTTT;TTTTTTTTT\t
         |AACGTGTA;TCCGTGATG\tThis is a condition
         |""".stripMargin
    val r = new BufferedReader(new StringReader(source))
    skipHeader(r, ReferenceData.LineRegex)
    assertEquals(r.readLine(), "TTTTTTTT;TTTTTTTTT\t")
  }

  test("skipHeader should not skip lines if there is no header, even if the ID is blank (quoted edition)") {
    val source =
      s"""\"TTTTTTTT;TTTTTTTTT\"\t\"\"
         |AACGTGTA;TCCGTGATG\tThis is a condition
         |""".stripMargin
    val r = new BufferedReader(new StringReader(source))
    skipHeader(r, ReferenceData.LineRegex)
    assertEquals(r.readLine(), "\"TTTTTTTT;TTTTTTTTT\"\t\"\"")
  }

end ParserPackageTest
