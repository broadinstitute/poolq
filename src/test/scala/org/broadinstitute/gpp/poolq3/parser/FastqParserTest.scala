/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import better.files._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class FastqParserTest extends AnyFlatSpec {

  "FastqParser" should "reject a malformed FASTQ file" in {
    val data =
      """@HWUSI-EAS100R:6:23:398:3989#1
        |AACTCACG
        |+
        |4<<8-767
        |@HWUSI-EAS100R:6:23:398:3989#1
        |AACTCACG
        |+""".stripMargin

    val file: File = File.newTemporaryFile("FastqParserTest", ".fastq")
    try {
      file.overwrite(data)
      val fqp = new FastqParser(file.path)
      intercept[InvalidFileException] {
        fqp.toList
      }
    } finally {
      file.delete()
    }
  }

  it should "parse complete records" in {
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
    try {
      file.overwrite(data)
      val fqp = new FastqParser(file.path)
      val fqi = fqp.iterator
      (fqi.toList should have).length(2)
      fqi.close()
    } finally {
      file.delete()
    }
  }

}
