package org.broadinstitute.gpp.poolq3.barcode

import munit.{FunSuite, ScalaCheckSuite}
import org.broadinstitute.gpp.poolq3.gen.{acgt, dnaSeqOfN}
import org.broadinstitute.gpp.poolq3.types.Read
import org.scalacheck.Gen
import org.scalacheck.Prop.forAllNoShrink

class SplitBarcodePolicyTest extends FunSuite with ScalaCheckSuite {

  test("find") {
    val ngen = Gen.listOf("N").map(_.mkString)

    val policy = SplitBarcodePolicy(
      prefix1 = "CACCG",
      b1Length = 7,
      gap = 10,
      prefix2 = "CCTGT",
      b2Length = 4,
      minPrefix1StartPos = None,
      maxPrefix1StartPos = None
    )

    forAllNoShrink(ngen, dnaSeqOfN(acgt, 7), dnaSeqOfN(acgt, 4), ngen) {
      (ns1: String, b1: String, b2: String, ns2: String) =>
        val seq = ns1 + "CACCG" + b1 + "NNNNNNNNNN" + "CCTGT" + b2 + ns2
        val read = Read("", seq)
        val found = policy.find(read)
        assertEquals(found.map(bc => new String(bc.barcode)), Some(b1 + b2))
    }
  }

}
