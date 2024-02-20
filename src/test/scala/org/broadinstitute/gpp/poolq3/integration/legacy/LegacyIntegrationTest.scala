/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.integration.legacy

import java.nio.file.Path

import better.files.*
import org.broadinstitute.gpp.poolq3.reports.PoolQ2Dialect
import org.broadinstitute.gpp.poolq3.testutil.contents
import org.broadinstitute.gpp.poolq3.{PoolQ, PoolQConfig, PoolQInput, PoolQOutput, TestResources}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*

class LegacyIntegrationTest extends AnyFlatSpec with TestResources:

  private[this] def filesSame(actual: Path, expected: Path): Unit =
    val _ = contents(actual) should be(contents(expected))

  /** Tests PoolQ end-to-end, using 10000 reads, 8 constructs, and 42 conditions. Compares the results to expected
    * results.
    *
    * This test runs with 36 base reads, so the construct sequence matched is only the first 20 bases.
    *
    * NOTE: the correctness of the results was verified by hand.
    */
  "PoolQ" should "testPoolQReads10000Reference8Conditions42" in {
    for
      countsFile <- File.temporaryFile("counts", ".txt")
      barcodeCountsFile <- File.temporaryFile("barcode-counts", ".txt")
      normalizedCountsFile <- File.temporaryFile("normcounts", ".txt")
      qualityFile <- File.temporaryFile("quality", ".txt")
      conditionBarcodeCountsSummaryFile <- File.temporaryFile("condition-barcode-counts-summary", ".txt")
      correlationFile <- File.temporaryFile("correlation", ".txt")
      unexpectedSequencesFile <- File.temporaryFile("unexpected", ".txt")
      unexpectedSequenceCacheDir <- File.temporaryDirectory("unexpected-cache")
      runInfoFile <- File.temporaryFile("runinfo", ".txt")
    do
      val config = PoolQConfig(
        input = PoolQInput(
          rowReference = resourcePath("reference-8.csv"),
          colReference = resourcePath("conditions-42.csv"),
          reads = Some((None, resourcePath("reads-10000.fastq")))
        ),
        output = PoolQOutput(
          countsFile = countsFile.toJava.toPath,
          normalizedCountsFile = normalizedCountsFile.toJava.toPath,
          barcodeCountsFile = barcodeCountsFile.toJava.toPath,
          qualityFile = qualityFile.toJava.toPath,
          conditionBarcodeCountsSummaryFile = conditionBarcodeCountsSummaryFile.toJava.toPath,
          correlationFile = correlationFile.toJava.toPath,
          unexpectedSequencesFile = unexpectedSequencesFile.toJava.toPath,
          runInfoFile = runInfoFile.toJava.toPath
        ),
        unexpectedSequenceCacheDir = Some(unexpectedSequenceCacheDir.toJava.toPath),
        rowBarcodePolicyStr = "FIXED@16:20",
        colBarcodePolicyStr = Some("FIXED@0"),
        reportsDialect = PoolQ2Dialect
      )

      val _ = PoolQ.run(config)

      filesSame(countsFile.path, resourcePath("counts-10000-8-42.txt"))
      filesSame(normalizedCountsFile.path, resourcePath("lognorm-10000-8-42.txt"))
      filesSame(barcodeCountsFile.path, resourcePath("barcode-counts-10000-8-42.txt"))
      filesSame(qualityFile.path, resourcePath("quality-10000-8-42.txt"))
      unexpectedSequenceCacheDir.exists should be(false)
  }

  "PoolQ" should "optionally not remove the unexpected sequence cache" in {
    for
      countsFile <- File.temporaryFile("counts", ".txt")
      barcodeCountsFile <- File.temporaryFile("barcode-counts", ".txt")
      normalizedCountsFile <- File.temporaryFile("normcounts", ".txt")
      qualityFile <- File.temporaryFile("quality", ".txt")
      conditionBarcodeCountsSummaryFile <- File.temporaryFile("condition-barcode-counts-summary", ".txt")
      correlationFile <- File.temporaryFile("correlation", ".txt")
      unexpectedSequencesFile <- File.temporaryFile("unexpected", ".txt")
      unexpectedSequenceCacheDir <- File.temporaryDirectory("unexpected-cache")
      runInfoFile <- File.temporaryFile("runinfo", ".txt")
    do
      val config = PoolQConfig(
        input = PoolQInput(
          rowReference = resourcePath("reference-8.csv"),
          colReference = resourcePath("conditions-42.csv"),
          reads = Some((None, resourcePath("reads-10000.fastq")))
        ),
        output = PoolQOutput(
          countsFile = countsFile.toJava.toPath,
          normalizedCountsFile = normalizedCountsFile.toJava.toPath,
          barcodeCountsFile = barcodeCountsFile.toJava.toPath,
          qualityFile = qualityFile.toJava.toPath,
          conditionBarcodeCountsSummaryFile = conditionBarcodeCountsSummaryFile.toJava.toPath,
          correlationFile = correlationFile.toJava.toPath,
          unexpectedSequencesFile = unexpectedSequencesFile.toJava.toPath,
          runInfoFile = runInfoFile.toJava.toPath
        ),
        unexpectedSequenceCacheDir = Some(unexpectedSequenceCacheDir.toJava.toPath),
        removeUnexpectedSequenceCache = false,
        rowBarcodePolicyStr = "FIXED@16:20",
        colBarcodePolicyStr = Some("FIXED@0"),
        reportsDialect = PoolQ2Dialect
      )

      val _ = PoolQ.run(config)

      filesSame(countsFile.path, resourcePath("counts-10000-8-42.txt"))
      filesSame(normalizedCountsFile.path, resourcePath("lognorm-10000-8-42.txt"))
      filesSame(barcodeCountsFile.path, resourcePath("barcode-counts-10000-8-42.txt"))
      filesSame(qualityFile.path, resourcePath("quality-10000-8-42.txt"))
      unexpectedSequenceCacheDir.exists should be(true)
  }

  /** Tests PoolQ end-to-end, using 42 base reads. The longer reads allow for comparison with the whole construct, and
    * also test the fact that PoolQ ignores any bases in the read that follow the construct.
    *
    * The "long_reads.fastq" file has the following properties: 4 reads for the first of 8 constructs; 1 read each for
    * constructs 2-7, and 0 reads for the last construct 1 read for the first construct and one read for the second
    * construct have a single base mismatch 1 read that matches the first construct, but with a barcode that is not in
    * the conditions file one successful read each for the first 10 conditions
    */
  it should "testPoolQLongerReads" in {
    for
      countsFile <- File.temporaryFile("counts", ".txt")
      normalizedCountsFile <- File.temporaryFile("normcounts", ".txt")
      barcodeCountsFile <- File.temporaryFile("barcode-counts", ".txt")
      qualityFile <- File.temporaryFile("quality", ".txt")
      conditionBarcodeCountsSummaryFile <- File.temporaryFile("condition-barcode-counts-summary", ".txt")
      correlationFile <- File.temporaryFile("correlation", ".txt")
      unexpectedSequencesFile <- File.temporaryFile("unexpected", ".txt")
      unexpectedSequenceCacheDir <- File.temporaryDirectory("unexpected-cache")
      runInfoFile <- File.temporaryFile("runinfo", ".txt")
    do
      val config = PoolQConfig(
        input = PoolQInput(
          rowReference = resourcePath("reference-8.csv"),
          colReference = resourcePath("conditions-42.csv"),
          reads = Some((None, resourcePath("long-reads.fastq")))
        ),
        output = PoolQOutput(
          countsFile = countsFile.toJava.toPath,
          normalizedCountsFile = normalizedCountsFile.toJava.toPath,
          barcodeCountsFile = barcodeCountsFile.toJava.toPath,
          qualityFile = qualityFile.toJava.toPath,
          conditionBarcodeCountsSummaryFile = conditionBarcodeCountsSummaryFile.toJava.toPath,
          correlationFile = correlationFile.toJava.toPath,
          unexpectedSequencesFile = unexpectedSequencesFile.toJava.toPath,
          runInfoFile = runInfoFile.toJava.toPath
        ),
        unexpectedSequenceCacheDir = Some(unexpectedSequenceCacheDir.toJava.toPath),
        rowBarcodePolicyStr = "FIXED@16:20",
        colBarcodePolicyStr = Some("FIXED@0"),
        reportsDialect = PoolQ2Dialect
      )

      val _ = PoolQ.run(config)

      filesSame(countsFile.toJava.toPath, resourcePath("long-reads-counts.txt"))
      filesSame(normalizedCountsFile.toJava.toPath, resourcePath("long-reads-lognorm.txt"))
      filesSame(barcodeCountsFile.toJava.toPath, resourcePath("long-reads-barcode-counts.txt"))
      filesSame(qualityFile.toJava.toPath, resourcePath("long-reads-quality.txt"))
  }

  /** Tests PoolQ end-to-end, using 42-base reads. The longer reads allow for comparison with the whole construct, and
    * also test the fact that PoolQ ignores any bases in the read that follow the construct.
    */
  it should "testPoolQMultipleBarcodesPerCondition" in {
    for
      countsFile <- File.temporaryFile("counts", ".txt")
      normalizedCountsFile <- File.temporaryFile("normcounts", ".txt")
      barcodeCountsFile <- File.temporaryFile("barcode-counts", ".txt")
      qualityFile <- File.temporaryFile("quality", ".txt")
      conditionBarcodeCountsSummaryFile <- File.temporaryFile("condition-barcode-counts-summary", ".txt")
      correlationFile <- File.temporaryFile("correlation", ".txt")
      unexpectedSequencesFile <- File.temporaryFile("unexpected", ".txt")
      unexpectedSequenceCacheDir <- File.temporaryDirectory("unexpected-cache")
      runInfoFile <- File.temporaryFile("runinfo", ".txt")
    do
      val config = PoolQConfig(
        input = PoolQInput(
          rowReference = resourcePath("reference-8.csv"),
          colReference = resourcePath("overlapping-barcode-conditions.csv"),
          reads = Some((None, resourcePath("long-reads.fastq")))
        ),
        output = PoolQOutput(
          countsFile = countsFile.toJava.toPath,
          normalizedCountsFile = normalizedCountsFile.toJava.toPath,
          barcodeCountsFile = barcodeCountsFile.toJava.toPath,
          qualityFile = qualityFile.toJava.toPath,
          conditionBarcodeCountsSummaryFile = conditionBarcodeCountsSummaryFile.toJava.toPath,
          correlationFile = correlationFile.toJava.toPath,
          unexpectedSequencesFile = unexpectedSequencesFile.toJava.toPath,
          runInfoFile = runInfoFile.toJava.toPath
        ),
        unexpectedSequenceCacheDir = Some(unexpectedSequenceCacheDir.toJava.toPath),
        rowBarcodePolicyStr = "FIXED@16:20",
        colBarcodePolicyStr = Some("FIXED@0"),
        reportsDialect = PoolQ2Dialect
      )

      val _ = PoolQ.run(config)

      filesSame(countsFile.toJava.toPath, resourcePath("overlapping-barcode-counts.txt"))
      filesSame(normalizedCountsFile.toJava.toPath, resourcePath("overlapping-barcode-lognorm.txt"))
      filesSame(barcodeCountsFile.toJava.toPath, resourcePath("overlapping-barcode-barcode-counts.txt"))
      filesSame(correlationFile.toJava.toPath, resourcePath("overlapping-barcode-correlation.txt"))
      filesSame(qualityFile.toJava.toPath, resourcePath("overlapping-barcode-quality.txt"))
  }

  /** Tests the case of multiplexed reads, where the reads are long enough to contain the entire construct barcode
    * sequence.
    */
  it should "testPoolQMultiplexedReads" in {
    for
      countsFile <- File.temporaryFile("counts", ".txt")
      normalizedCountsFile <- File.temporaryFile("normcounts", ".txt")
      barcodeCountsFile <- File.temporaryFile("barcode-counts", ".txt")
      qualityFile <- File.temporaryFile("quality", ".txt")
      conditionBarcodeCountsSummaryFile <- File.temporaryFile("condition-barcode-counts-summary", ".txt")
      correlationFile <- File.temporaryFile("correlation", ".txt")
      unexpectedSequencesFile <- File.temporaryFile("unexpected", ".txt")
      unexpectedSequenceCacheDir <- File.temporaryDirectory("unexpected-cache")
      runInfoFile <- File.temporaryFile("runinfo", ".txt")
    do
      val config = PoolQConfig(
        input = PoolQInput(
          rowReference = resourcePath("next500-reference.txt"),
          colReference = resourcePath("next500-conditions.txt"),
          rowReads = Some((None, resourcePath("next500-construct.fastq"))),
          colReads = Some(resourcePath("next500-dmux.fastq"))
        ),
        output = PoolQOutput(
          countsFile = countsFile.toJava.toPath,
          normalizedCountsFile = normalizedCountsFile.toJava.toPath,
          barcodeCountsFile = barcodeCountsFile.toJava.toPath,
          qualityFile = qualityFile.toJava.toPath,
          conditionBarcodeCountsSummaryFile = conditionBarcodeCountsSummaryFile.toJava.toPath,
          correlationFile = correlationFile.toJava.toPath,
          unexpectedSequencesFile = unexpectedSequencesFile.toJava.toPath,
          runInfoFile = runInfoFile.toJava.toPath
        ),
        unexpectedSequenceCacheDir = Some(unexpectedSequenceCacheDir.toJava.toPath),
        rowBarcodePolicyStr = "PREFIX:ACCG@7:20",
        colBarcodePolicyStr = Some("FIXED@0"),
        reportsDialect = PoolQ2Dialect
      )

      val _ = PoolQ.run(config)

      filesSame(countsFile.toJava.toPath, resourcePath("next500-counts.txt"))
      filesSame(qualityFile.toJava.toPath, resourcePath("next500-quality.txt"))
  }

  it should "testPoolQMultiplexedShortReads" in {
    for
      countsFile <- File.temporaryFile("counts", ".txt")
      normalizedCountsFile <- File.temporaryFile("normcounts", ".txt")
      barcodeCountsFile <- File.temporaryFile("barcode-counts", ".txt")
      qualityFile <- File.temporaryFile("quality", ".txt")
      conditionBarcodeCountsSummaryFile <- File.temporaryFile("condition-barcode-counts-summary", ".txt")
      correlationFile <- File.temporaryFile("correlation", ".txt")
      unexpectedSequencesFile <- File.temporaryFile("unexpected", ".txt")
      unexpectedSequenceCacheDir <- File.temporaryDirectory("unexpected-cache")
      runInfoFile <- File.temporaryFile("runinfo", ".txt")
    do
      val config = PoolQConfig(
        input = PoolQInput(
          rowReference = resourcePath("next500-reference.txt"),
          colReference = resourcePath("next500-conditions.txt"),
          rowReads = Some((None, resourcePath("next500-construct-short.fastq"))),
          colReads = Some(resourcePath("next500-dmux.fastq"))
        ),
        output = PoolQOutput(
          countsFile = countsFile.toJava.toPath,
          normalizedCountsFile = normalizedCountsFile.toJava.toPath,
          barcodeCountsFile = barcodeCountsFile.toJava.toPath,
          qualityFile = qualityFile.toJava.toPath,
          conditionBarcodeCountsSummaryFile = conditionBarcodeCountsSummaryFile.toJava.toPath,
          correlationFile = correlationFile.toJava.toPath,
          unexpectedSequencesFile = unexpectedSequencesFile.toJava.toPath,
          runInfoFile = runInfoFile.toJava.toPath
        ),
        unexpectedSequenceCacheDir = Some(unexpectedSequenceCacheDir.toJava.toPath),
        rowBarcodePolicyStr = "PREFIX:ACCG@7:19",
        colBarcodePolicyStr = Some("FIXED@0"),
        reportsDialect = PoolQ2Dialect
      )

      val _ = PoolQ.run(config)

      filesSame(countsFile.toJava.toPath, resourcePath("next500-counts.txt"))
      filesSame(qualityFile.toJava.toPath, resourcePath("next500-quality.txt"))
  }

  /** Tests PoolQ end-to-end, using 42 base reads. The longer reads allow for comparison with the whole construct, and
    * also tests the fact that PoolQ ignores any bases in the read that follow the construct. In this example, one
    * barcode maps to multiple construct IDs.
    */
  it should "testPoolQDuplicateConstructs" in {
    for
      countsFile <- File.temporaryFile("counts", ".txt")
      normalizedCountsFile <- File.temporaryFile("normcounts", ".txt")
      barcodeCountsFile <- File.temporaryFile("barcode-counts", ".txt")
      qualityFile <- File.temporaryFile("quality", ".txt")
      conditionBarcodeCountsSummaryFile <- File.temporaryFile("condition-barcode-counts-summary", ".txt")
      correlationFile <- File.temporaryFile("correlation", ".txt")
      unexpectedSequencesFile <- File.temporaryFile("unexpected", ".txt")
      unexpectedSequenceCacheDir <- File.temporaryDirectory("unexpected-cache")
      runInfoFile <- File.temporaryFile("runinfo", ".txt")
    do
      val config = PoolQConfig(
        input = PoolQInput(
          rowReference = resourcePath("reference-9.csv"),
          colReference = resourcePath("conditions-42.csv"),
          reads = Some((None, resourcePath("long-reads.fastq")))
        ),
        output = PoolQOutput(
          countsFile = countsFile.toJava.toPath,
          normalizedCountsFile = normalizedCountsFile.toJava.toPath,
          barcodeCountsFile = barcodeCountsFile.toJava.toPath,
          qualityFile = qualityFile.toJava.toPath,
          conditionBarcodeCountsSummaryFile = conditionBarcodeCountsSummaryFile.toJava.toPath,
          correlationFile = correlationFile.toJava.toPath,
          unexpectedSequencesFile = unexpectedSequencesFile.toJava.toPath,
          runInfoFile = runInfoFile.toJava.toPath
        ),
        unexpectedSequenceCacheDir = Some(unexpectedSequenceCacheDir.toJava.toPath),
        rowBarcodePolicyStr = "FIXED@16",
        colBarcodePolicyStr = Some("FIXED@0"),
        reportsDialect = PoolQ2Dialect
      )

      val _ = PoolQ.run(config)

      filesSame(countsFile.toJava.toPath, resourcePath("duplicate-hp-counts.txt"))
      filesSame(normalizedCountsFile.toJava.toPath, resourcePath("duplicate-hp-lognorm.txt"))
      filesSame(barcodeCountsFile.toJava.toPath, resourcePath("duplicate-hp-barcode-counts.txt"))
      filesSame(qualityFile.toJava.toPath, resourcePath("duplicate-hp-quality.txt"))
  }

end LegacyIntegrationTest
