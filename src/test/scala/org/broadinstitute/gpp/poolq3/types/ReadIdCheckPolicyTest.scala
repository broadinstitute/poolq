/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.types

import munit.FunSuite

class ReadIdCheckPolicyTest extends FunSuite:

  val pe1 = Read("@SL-HDG:HL3FLBCX3210428:HL3FLBCX3:1:1101:10000:10930 1:N:0:", "")
  val pe2 = Read("@SL-HDG:HL3FLBCX3210428:HL3FLBCX3:1:1101:10000:10930 2:N:0:", "")
  val pe3 = Read("@SL-HDG:HL3FLBCX3210428:HL3FLBCX3:1:1101:10000:10930 :N:0:", "")
  val wut = Read("@WUT?", "")

  val pes = List(pe1, pe2, pe3)

  val pairedEndTuples =
    for
      a <- pes
      b <- pes if a != b
    yield (a, b)

  test("Illumina policy checks up to the first space") {
    pairedEndTuples.foreach { case (a, b) => ReadIdCheckPolicy.Illumina.check(a, b) }
    pes.foreach(r => intercept[UncoordinatedReadsException](ReadIdCheckPolicy.Illumina.check(r, wut)))
  }

  test("Lax policy doesn't check anything") {
    List.concat(pairedEndTuples, List((pe1, wut), (pe2, wut), (pe3, wut))).foreach { case (a, b) =>
      ReadIdCheckPolicy.Lax.check(a, b)
    }
  }

  test("Strict policy ensures IDs are equal") {
    pes.zip(pes).foreach { case (a, b) => ReadIdCheckPolicy.Strict.check(a, b) }
    pairedEndTuples.foreach { case (a, b) =>
      intercept[UncoordinatedReadsException](ReadIdCheckPolicy.Strict.check(a, b))
    }
  }

end ReadIdCheckPolicyTest
