/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.nio.file.{Files, Paths}

import scala.io.Source
import scala.util.Using

import munit.FunSuite
import org.broadinstitute.gpp.poolq3.{BuildInfo, PoolQConfig, PoolQInput, PoolQOutput}

class RunInfoWriterTest extends FunSuite {

  test("runinfo") {
    val outputFile = Files.createTempFile("runinfo", ".txt")
    try {
      val config = PoolQConfig(
        input = PoolQInput(
          rowReference = Paths.get("/gpp/reference/reference_20191115.csv"),
          colReference = Paths.get("/gpp/experiments/conditions_20191115_USE_THIS_ONE.csv"),
          globalReference = Some(Paths.get("/gpp/reference/every_sgrna_ever.csv")),
          rowReads = Some(Paths.get("/sequencing/walkoff/5/fastq5.fastq.gz")),
          reverseRowReads = Some(Paths.get("/sequencing/walkoff/5/fastq5.1.fastq.gz")),
          colReads = Some(Paths.get("/sequencing/walkoff/5/fastq5_barcode.fastq.gz"))
        ),
        output = PoolQOutput(
          countsFile = Paths.get("/var/tmp/counts.txt"),
          normalizedCountsFile = Paths.get("/var/tmp/lognormalized-counts.txt"),
          barcodeCountsFile = Paths.get("/var/tmp/barcode-counts.txt"),
          qualityFile = Paths.get("/var/tmp/quality.txt"),
          correlationFile = Paths.get("/var/tmp/correlation.txt"),
          unexpectedSequencesFile = Paths.get("/var/tmp/unexpected-sequences.txt"),
          runInfoFile = Paths.get("/var/tmp/runinfo.txt")
        ),
        rowBarcodePolicyStr = "PREFIX:CACCG@18",
        reverseRowBarcodePolicyStr = Some("PREFIX:GCCAC@13"),
        colBarcodePolicyStr = Some("FIXED@0"),
        reportsDialect = PoolQ2Dialect
      )
      val _ = RunInfoWriter.write(outputFile, config)

      val expected =
        s"""PoolQ version: ${BuildInfo.version}
           |PoolQ command-line settings:
           |  --row-reference reference_20191115.csv \\
           |  --col-reference conditions_20191115_USE_THIS_ONE.csv \\
           |  --global-reference every_sgrna_ever.csv \\
           |  --row-reads fastq5.fastq.gz \\
           |  --rev-row-reads fastq5.1.fastq.gz \\
           |  --col-reads fastq5_barcode.fastq.gz \\
           |  --read-id-check-policy strict \\
           |  --row-matcher mismatch \\
           |  --col-matcher exact \\
           |  --row-barcode-policy PREFIX:CACCG@18 \\
           |  --rev-row-barcode-policy PREFIX:GCCAC@13 \\
           |  --col-barcode-policy FIXED@0 \\
           |  --unexpected-sequence-threshold 100 \\
           |  --compat \\
           |  --counts counts.txt \\
           |  --normalized-counts lognormalized-counts.txt \\
           |  --barcode-counts barcode-counts.txt \\
           |  --quality quality.txt \\
           |  --correlation correlation.txt \\
           |  --unexpected-sequences unexpected-sequences.txt \\
           |  --run-info runinfo.txt
           |""".stripMargin

      val actual = Using.resource(Source.fromFile(outputFile.toFile))(_.getLines().mkString("\n"))
      assertEquals(actual, expected)
    } finally {
      Files.delete(outputFile)
    }

  }

}
