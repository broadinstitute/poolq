/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reference

import org.apache.commons.text.similarity.LevenshteinDistance
import org.broadinstitute.gpp.poolq3.gen.barcode
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks._

class BkTreeTest extends AnyFlatSpec {
  import Gen.listOfN

  val referenceGen: Gen[List[String]] = listOfN(1000, barcode)

  def levenshtein(x: String, y: String): Int = LevenshteinDistance.getDefaultInstance.apply(x, y)

  "BKTree" should "represent a small dictionary" in {
    val t =
      new BkTree(levenshtein, Seq("book", "books", "cake", "boo", "boon", "cook", "cape", "cart"))

    val _ = t.query("book", 1) should be(Set("book", "books", "boon", "boo", "cook"))
    t.query("booky", 1) should be(Set("book", "books"))
  }

  it should "support queries of arbitrary dictionaries" in {
    forAll(referenceGen) { (reference: List[String]) =>
      val tree = new BkTree(levenshtein, reference)
      reference.headOption.foreach { query =>
        (0 until query.length).foreach { i =>
          val array = query.toCharArray
          array(i) = 'N'
          val results = tree.query(array.mkString, 1)
          results should contain(query)
        }
      }
    }
  }

}
