/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.nio.file.Files

import munit.FunSuite
import org.broadinstitute.gpp.poolq3.TestResources
import org.broadinstitute.gpp.poolq3.hist.{BasicShardedHistogram, OpenHashMapHistogram, TupleHistogram}
import org.broadinstitute.gpp.poolq3.process.State
import org.broadinstitute.gpp.poolq3.testutil.contents

class UmiQualityTest extends FunSuite with TestResources {

  test("umi quality report") {
    val state = new State(
      new BasicShardedHistogram(new TupleHistogram),
      new OpenHashMapHistogram,
      new OpenHashMapHistogram,
      new OpenHashMapHistogram
    )

    val umi: LazyList[String] =
      for {
        b1 <- LazyList('A', 'C', 'G', 'T')
        b2 <- LazyList('A', 'C', 'G', 'T')
        b3 <- LazyList('A', 'C', 'G', 'T')
        b4 <- LazyList('A', 'C', 'G', 'T')
        b5 <- LazyList('A', 'C', 'G', 'T')
        b6 <- LazyList('A', 'C', 'G', 'T')
      } yield s"$b1$b2$b3$b4$b5$b6"

    val (expectedUmiLL, rest) = umi.splitAt(96)
    val expectedUmi = expectedUmiLL.toList.zipWithIndex.map { case (x, i) => (x, i + 1) }
    val unexpectedUmi = rest.take(200).toList.zipWithIndex.map { case (x, i) => (x, i + 1) }

    // build up the state used by the UMI report writer
    expectedUmi.foreach { case (b, i) =>
      val u = Some(b)
      val t = ("AAAAAAAAAA", "TTTT")
      (1 to i).foreach(_ => state.known.increment(u, t))
    }
    unexpectedUmi.foreach { case (u, i) =>
      (1 to i).foreach(_ => state.unknownUmi.increment(u))
    }

    val file = Files.createTempFile("umi-quality-", ".txt")
    try {
      val _ = UmiQualityWriter.write(file, state)

      // read the file, split into lines, drop the 1st header
      val reportContents = contents(file).split("\n", -1).drop(1)

      val (region1, rest) = reportContents.splitAt(96)
      region1.zip(expectedUmi).foreach { case (a, e) =>
        assertEquals(a, s"${e._1}\t${e._2}")
      }

      rest.slice(2, 102).zip(unexpectedUmi.reverse.take(100)).foreach { case (a, e) =>
        assertEquals(a, s"${e._1}\t${e._2}")
      }
    } finally {
      val _ = Files.deleteIfExists(file)
    }

  }

}
