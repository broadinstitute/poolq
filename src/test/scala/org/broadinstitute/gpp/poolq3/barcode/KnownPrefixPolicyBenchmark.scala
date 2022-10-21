/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import org.broadinstitute.gpp.poolq3.types.Read
import org.scalatest.flatspec.AnyFlatSpec

class KnownPrefixPolicyBenchmark extends AnyFlatSpec {

  ignore should "compare times" in {
    val seqs = Seq(
      "AGCTTGTGGAAAGGACGAAACACCGGCGCTGGTAGTTCGAACCCGGTTTT",
      "GCTTGTGGAAAGGACGAAACACCGGGGCCTCTCACCTTCCACGAGTTTTA",
      "GCTTGTGGAAAGGACGAAACACCGGCAGTTCTCATCCCTCTCATGTTTTA",
      "AGCTTGTGGAAAGGACGAAACACCGTGGCCGGCGACAAGGAGCCGGTTTT"
    )
    val reads = seqs.map(Read("id", _))

    val kmp = KmpKnownPrefixPolicy("CACCG:7", 20)
    val indexof = IndexOfKnownPrefixPolicy("CACCG:7", 20)

    val range = 1 to 1000000

    Thread.sleep(1000)

    // time policy2
    range.foreach(i => indexof.find(reads(i % 4)))
    val indexoft0 = System.currentTimeMillis()
    range.foreach(i => indexof.find(reads(i % 4)))
    val indexoft1 = System.currentTimeMillis()

    Thread.sleep(1000)

    // time policy1
    range.foreach(i => kmp.find(reads(i % 4)))
    val kmpt0 = System.currentTimeMillis()
    range.foreach(i => kmp.find(reads(i % 4)))
    val kmpt1 = System.currentTimeMillis()

    println(s"KMP:     ${kmpt1 - kmpt0} ms")
    println(s"IndexOf: ${indexoft1 - indexoft0} ms")

  }

}
