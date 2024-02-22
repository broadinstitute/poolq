/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.integration

import java.nio.file.Path as JPath

import cats.effect.{IO, Resource}
import fs2.io.file.{Files, Path}
import fs2.{Stream, text}
import munit.CatsEffectSuite
import org.broadinstitute.gpp.poolq3.reports.PoolQ2Dialect
import org.broadinstitute.gpp.poolq3.testutil.tempFile
import org.broadinstitute.gpp.poolq3.{PoolQ, PoolQConfig, PoolQInput, PoolQOutput, TestResources}

class UnlabeledConditionsTest extends CatsEffectSuite with TestResources:

  val outputFilesResources: Resource[IO, PoolQOutput] =
    for
      countsFile <- tempFile[IO]("counts", ".txt")
      barcodeCountsFile <- tempFile[IO]("barcode-counts", ".txt")
      normalizedCountsFile <- tempFile[IO]("normcounts", ".txt")
      qualityFile <- tempFile[IO]("quality", ".txt")
      conditionBarcodeCountsSummaryFile <- tempFile[IO]("condition-barcode-counts-summary", ".txt")
      correlationFile <- tempFile[IO]("correlation", ".txt")
      unexpectedSequencesFile <- tempFile[IO]("unexpected", ".txt")
      runInfoFile <- tempFile[IO]("runinfo", ".txt")
    yield PoolQOutput(
      countsFile = countsFile,
      normalizedCountsFile = normalizedCountsFile,
      barcodeCountsFile = barcodeCountsFile,
      qualityFile = qualityFile,
      conditionBarcodeCountsSummaryFile = conditionBarcodeCountsSummaryFile,
      correlationFile = correlationFile,
      unexpectedSequencesFile = unexpectedSequencesFile,
      runInfoFile = runInfoFile
    )

  test("Unlabeled sample barcodes aggregate together") {

    outputFilesResources.use { poolQOutput =>
      val config = PoolQConfig(
        input = PoolQInput(
          rowReference = resourcePath("reference.csv"),
          colReference = resourcePath("unlabeled-conditions.csv"),
          reads = Some((None, resourcePath("reads.fastq")))
        ),
        output = poolQOutput,
        unexpectedSequenceCacheDir = None,
        rowBarcodePolicyStr = "FIXED@3",
        colBarcodePolicyStr = Some("FIXED@0"),
        reportsDialect = PoolQ2Dialect
      )

      IO.blocking(PoolQ.run(config)) >>
        filesSame(resourcePath("unlabeled-expected-counts.txt"), poolQOutput.countsFile)
    }

  }

  def filesSame(expected: JPath, actual: JPath)(implicit loc: munit.Location): IO[Unit] =
    val ef: Stream[IO, String] = Files[IO].readAll(Path.fromNioPath(expected)).through(text.utf8.decode).foldMonoid
    val af: Stream[IO, String] = Files[IO].readAll(Path.fromNioPath(actual)).through(text.utf8.decode).foldMonoid

    ef.zip(af).map { case (e, a) => assertEquals(e, a) }.compile.drain

end UnlabeledConditionsTest
