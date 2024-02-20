/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.{PrintWriter, StringWriter}
import java.nio.file.Paths

import munit.{FunSuite, ScalaCheckSuite}
import org.broadinstitute.gpp.poolq3.parser.ReferenceEntry
import org.broadinstitute.gpp.poolq3.reference.{ExactReference, Reference}
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

class ReportsTest extends FunSuite with ScalaCheckSuite:

  test("writeRowIdentifiers maps barcodes back to their original form") {
    val reference: Reference =
      ExactReference(Seq(ReferenceEntry("CTC:GAG", "Stem Loop")), identity, includeAmbiguous = false)
    val rowBc = "CTCGAG"
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    writeRowIdentifiers(reference, rowBc, pw)
    assertEquals(sw.toString, "CTC:GAG\tStem Loop\t")
  }

  test("parseFilename") {
    assertEquals(parseFilename(Paths.get("/foo/bar/baz")), ParsedFilename(Paths.get("/foo/bar"), "baz", None))
    assertEquals(
      parseFilename(Paths.get("/foo/baz.txt.html")),
      ParsedFilename(Paths.get("/foo"), "baz.txt", Some(".html"))
    )
  }

  property("topN") {
    forAll(Gen.listOf(Gen.posNum[Int]), Gen.posNum[Int]) { (xs, n) =>
      val actual = topN(xs, n)
      val expected = xs.sorted.reverse.take(n)
      assertEquals(actual, expected)
    }
  }

end ReportsTest
