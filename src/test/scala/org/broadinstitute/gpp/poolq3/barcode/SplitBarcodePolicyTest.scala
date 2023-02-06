package org.broadinstitute.gpp.poolq3.barcode

import munit.{FunSuite, ScalaCheckSuite}
import org.broadinstitute.gpp.poolq3.gen.{acgt, acgtn, dnaSeq, dnaSeqOfN}
import org.broadinstitute.gpp.poolq3.types.Read
import org.scalacheck.Gen
import org.scalacheck.Prop.forAllNoShrink

class SplitBarcodePolicyTest extends FunSuite with ScalaCheckSuite {

  private val ngen = Gen.listOf("N").map(_.mkString)

  test("find") {
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

  test("equivalency") {
    val spb = SplitBarcodePolicy(
      prefix1 = "CACCG",
      b1Length = 20,
      gap = 50,
      prefix2 = "GAAAC",
      b2Length = 13,
      minPrefix1StartPos = None,
      maxPrefix1StartPos = None
    )

    val gtbp = GeneralTemplatePolicy(
      KeyMask("caccgNNNNNNNNNNNNNNNNNNNNnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnngaaacNNNNNNNNNNNNN"),
      None,
      None
    )

    val prefixGen1 = Gen.oneOf(Gen.const("CACCG"), dnaSeqOfN(acgtn, 5))
    val prefixGen2 = Gen.oneOf(Gen.const("GAAAC"), dnaSeqOfN(acgtn, 5))

    forAllNoShrink(
      dnaSeq(acgtn),
      prefixGen1,
      dnaSeqOfN(acgt, 20),
      dnaSeqOfN(acgtn, 50),
      prefixGen2,
      dnaSeqOfN(acgtn, 13),
      dnaSeq(acgtn)
    ) { (fpFlank: String, p1: String, b1: String, gapSeq: String, p2: String, b2: String, tpFlank: String) =>
      val seq = fpFlank + p1 + b1 + gapSeq + p2 + b2 + tpFlank
      val read = Read("", seq)

      val actual = spb.find(read)
      assertEquals(actual, gtbp.find(read))
    }
  }

}
