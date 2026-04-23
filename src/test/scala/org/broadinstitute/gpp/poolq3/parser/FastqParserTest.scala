/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import scala.util.Using

import better.files.*
import munit.FunSuite

class FastqParserTest extends FunSuite:

  test("FastqParser should reject a malformed FASTQ file") {
    val data =
      """@HWUSI-EAS100R:6:23:398:3989#1
        |AACTCACG
        |+
        |4<<8-767
        |@HWUSI-EAS100R:6:23:398:3989#1
        |AACTCACG
        |+""".stripMargin

    val file: File = File.newTemporaryFile("FastqParserTest", ".fastq")
    try
      file.overwrite(data)
      intercept[InvalidFileException] {
        Using.resource(new FastqParser(file.path).iterator)(iter => iter.toList)
      }
    finally file.delete()
  }

  test("FastqParser should reject a misaligned FASTQ file") {
    val data =
      """+
        |4<<8-767
        |@HWUSI-EAS100R:6:23:398:3989#1
        |AACTCACG
        |+""".stripMargin

    val file: File = File.newTemporaryFile("FastqParserTest", ".fastq")
    try
      file.overwrite(data)
      intercept[InvalidFileException] {
        Using.resource(new FastqParser(file.path).iterator)(iter => iter.toList)
      }
    finally file.delete()
  }

  test("should parse complete records") {
    val data =
      """@HWUSI-EAS100R:6:23:398:3989#1
        |AACTCACG
        |+
        |4<<8-767
        |@HWUSI-EAS100R:6:23:398:3989#2
        |TTGAACCG
        |+
        |=@975@<7""".stripMargin
    val file: File = File.newTemporaryFile("FastqParserTest", ".fastq")
    try
      file.overwrite(data)
      Using.resource(new FastqParser(file.path).iterator)(iter => assertEquals(iter.toList.length, 2))
    finally file.delete()
  }

  test("should reject a file ending with only 1 line") {
    val data = "@HWUSI-EAS100R:6:23:398:3989#1"
    val file: File = File.newTemporaryFile("FastqParserTest", ".fastq")
    try
      file.overwrite(data)
      intercept[InvalidFileException] {
        Using.resource(new FastqParser(file.path).iterator)(iter => iter.toList)
      }
    finally file.delete()
  }

end FastqParserTest
