/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import cats.effect.IO
import fs2.io.file.Files
import munit.CatsEffectSuite
import org.broadinstitute.gpp.poolq3.hist.{BasicShardedHistogram, OpenHashMapHistogram, TupleHistogram}
import org.broadinstitute.gpp.poolq3.parser.ReferenceEntry
import org.broadinstitute.gpp.poolq3.process.State
import org.broadinstitute.gpp.poolq3.reference.ExactReference

class ConditionBarcodeCountsSummaryTest extends CatsEffectSuite:

  private val Condition1 = "DMSO"
  private val Condition2 = "ITMFA"
  private val Condition3 = "No Drug"
  private val SampleBarcode1 = "GTAT"
  private val SampleBarcode2 = "ACAT"
  private val SampleBarcode3 = "TCAG"
  private val SampleBarcode4 = "TCCG"

  // we only need 1 construct to populate the report
  private val Construct1 = "AACCGGTTAACCGGTTTTAAG"
  private val ConstructId1 = "BRDN01"

  private val Constructs = List(ReferenceEntry(Construct1, ConstructId1))

  private val rowReference = ExactReference(Constructs, identity, includeAmbiguous = false)

  private val colReference =
    ExactReference(
      List(
        ReferenceEntry(SampleBarcode1, Condition1),
        ReferenceEntry(SampleBarcode2, Condition2),
        ReferenceEntry(SampleBarcode3, Condition3),
        ReferenceEntry(SampleBarcode4, Condition3)
      ),
      identity,
      includeAmbiguous = false
    )

  def emptyState(): State =
    new State(
      new BasicShardedHistogram[String, (String, String)](new TupleHistogram()),
      new OpenHashMapHistogram(),
      new OpenHashMapHistogram(),
      new OpenHashMapHistogram()
    )

  test("condition barcode counts summary") {
    val sample1MatchesBoth = 10
    val sample2MatchesBoth = 7
    val sample3MatchesBoth = 28
    val sample4MatchesBoth = 3

    val sample1MatchesCol = sample1MatchesBoth + 8
    val sample2MatchesCol = sample2MatchesBoth + 3
    val sample3MatchesCol = sample3MatchesBoth + 17
    val sample4MatchesCol = sample4MatchesBoth + 11

    Files[IO].tempDirectory.use { tmpDir =>
      val cbcs = tmpDir / "cbcs.txt"

      // fill out state
      val state = emptyState()
      0.until(sample1MatchesBoth).foreach(_ => state.known.increment(None, (Construct1, SampleBarcode1)))
      0.until(sample2MatchesBoth).foreach(_ => state.known.increment(None, (Construct1, SampleBarcode2)))
      0.until(sample3MatchesBoth).foreach(_ => state.known.increment(None, (Construct1, SampleBarcode3)))
      0.until(sample4MatchesBoth).foreach(_ => state.known.increment(None, (Construct1, SampleBarcode4)))

      0.until(sample1MatchesCol).foreach(_ => state.knownCol.increment(SampleBarcode1))
      0.until(sample2MatchesCol).foreach(_ => state.knownCol.increment(SampleBarcode2))
      0.until(sample3MatchesCol).foreach(_ => state.knownCol.increment(SampleBarcode3))
      0.until(sample4MatchesCol).foreach(_ => state.knownCol.increment(SampleBarcode4))

      state.reads = sample1MatchesCol + sample2MatchesCol + sample3MatchesCol + sample4MatchesCol + 5

      IO.blocking {
        QualityWriter
          .write((tmpDir / "quality.txt").toNioPath, cbcs.toNioPath, state, rowReference, colReference, false)
          .get
      } >>
        Files[IO]
          .readUtf8(cbcs)
          .compile
          .lastOrError
          .assertEquals(
            """Barcode	Condition	Matched (Construct+Sample Barcode)	Matched Sample Barcode	% Match	Normalized Match
              |GTAT	DMSO	10	18	55.56	16.730
              |ACAT	ITMFA	7	10	70.00	16.215
              |TCAG	No Drug	28	45	62.22	18.215
              |TCCG	No Drug	3	14	21.43	14.993
              |""".stripMargin
          )

    }

  }

end ConditionBarcodeCountsSummaryTest
