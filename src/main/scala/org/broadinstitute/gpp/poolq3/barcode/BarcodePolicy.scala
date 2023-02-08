/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import java.util.Arrays.copyOfRange

import scala.annotation.{switch, tailrec}
import scala.util.matching.Regex

import org.broadinstitute.gpp.poolq3.types.Read

sealed trait BarcodePolicy {
  def length: Int
  def find(read: Read): Option[FoundBarcode]
}

object BarcodePolicy {
  private val Regex = """^([A-Z]+)(.+)""".r

  def apply(desc: String, refBarcodeLength: Int, skipShortReads: Boolean): BarcodePolicy =
    desc match {
      case Regex(descriptor, rest) =>
        descriptor match {
          case "FIXED"                => FixedOffsetPolicy(rest, refBarcodeLength, skipShortReads)
          case "PREFIX"               => KnownPrefixPolicy(rest, refBarcodeLength)
          case "KEYMASK" | "TEMPLATE" => TemplatePolicy(rest, refBarcodeLength)
          case _ =>
            throw new IllegalArgumentException(s"Unrecognized barcode policy: $desc")
        }
      case _ => throw new IllegalArgumentException(s"Unrecognized barcode policy: $desc")
    }

}

final case class FixedOffsetPolicy(startPos0: Int, length: Int, skipShortReads: Boolean) extends BarcodePolicy {
  private[this] val minLength: Int = startPos0 + length
  private[this] val endPos0: Int = startPos0 + length

  override def find(read: Read): Option[FoundBarcode] =
    if (read.seq.length < minLength) {
      if (skipShortReads) None
      else throw ReadTooShortException(read.seq, startPos0, minLength)
    } else Some(FoundBarcode(copyOfRange(read.seq.toCharArray, startPos0, endPos0), startPos0))

}

object FixedOffsetPolicy {
  private[this] val Regex1: Regex = """^[:@](\d+)$""".r
  private[this] val Regex2: Regex = """^[:@](\d+):(\d+)$""".r

  def apply(s: String, refBarcodeLength: Int, skipShortReads: Boolean): FixedOffsetPolicy =
    s match {
      case Regex1(sp0) =>
        FixedOffsetPolicy(sp0.toInt, refBarcodeLength, skipShortReads)
      case Regex2(sp0, len) =>
        FixedOffsetPolicy(sp0.toInt, len.toInt, skipShortReads)
      case _ =>
        throw new IllegalArgumentException(s"Incomprehensible fixed offset barcode policy: $s")
    }

}

/** This is a marker trait for known-prefix policies; it exists primarily for its companion object, defined below.
  */
sealed trait KnownPrefixPolicy extends BarcodePolicy {
  def prefix: String
}

final case class IndexOfKnownPrefixPolicy(
  prefix: String,
  length: Int,
  minPrefixStartPos: Option[Int] = None,
  maxPrefixStartPos: Option[Int] = None
) extends KnownPrefixPolicy {

  private[this] val prefixLength: Int = prefix.length
  private[this] val minPrefixStartPosInt: Int = minPrefixStartPos.getOrElse(0)
  private[this] val maxPrefixStartPosInt: Int = maxPrefixStartPos.getOrElse(Int.MaxValue)

  override def find(read: Read): Option[FoundBarcode] = {
    val index = read.seq.indexOf(prefix, minPrefixStartPosInt)
    val barcodeStart = index + prefixLength
    if (index < 0) None
    else if (index > maxPrefixStartPosInt) None
    else if ((read.seq.length - barcodeStart) < length) None
    else Some(FoundBarcode(copyOfRange(read.seq.toCharArray, barcodeStart, barcodeStart + length), barcodeStart))
  }

}

final case class KmpKnownPrefixPolicy(
  prefix: String,
  length: Int,
  minPrefixStartPos: Option[Int] = None,
  maxPrefixStartPos: Option[Int] = None
) extends KnownPrefixPolicy {

  private[this] val kmp: KnuthMorrisPratt = new KnuthMorrisPratt(prefix)
  private[this] val prefixLength: Int = prefix.length
  private[this] val minPrefixStartPosInt: Int = minPrefixStartPos.getOrElse(0)
  private[this] val maxPrefixStartPosInt: Int = maxPrefixStartPos.getOrElse(Int.MaxValue)

  override def find(read: Read): Option[FoundBarcode] = {
    val searchEndPos = math.min(maxPrefixStartPosInt, read.seq.length - length)
    kmp.search(read.seq, minPrefixStartPosInt, searchEndPos).map { prefixStart =>
      val barcodeStart = prefixStart + prefixLength
      FoundBarcode(copyOfRange(read.seq.toCharArray, barcodeStart, barcodeStart + length), barcodeStart)
    }
  }

}

object KnownPrefixPolicy {
  val Regex: Regex = """^:([ACGT]+)(?:@(\d+)?(-\d+)?)?(:\d+)?$""".r

  def apply(s: String, refBarcodeLength: Int): KnownPrefixPolicy =
    s match {
      case Regex(prefix, minStr, maxStr, lengthStr) =>
        val min = Option(minStr).map(_.toInt)
        val max = Option(maxStr).map(_.tail.toInt)
        val length = Option(lengthStr).map(_.tail.toInt).getOrElse(refBarcodeLength)
        IndexOfKnownPrefixPolicy(prefix, length, min, max)
      case _ =>
        throw new IllegalArgumentException(s"Incomprehensible known prefix barcode policy: $s")
    }

}

sealed trait TemplatePolicy extends BarcodePolicy with Product with Serializable

object TemplatePolicy {

  val Regex1: Regex = """^:([ACGTRYSWKMBDHVNacgtryswkmbdhvn]+)(?:@(\d+)?(-\d+)?)?$""".r
  val Regex2: Regex = """^([acgt]+)(N+)(n+)([acgt]+)(N+)[acgt]*$""".r

  def apply(s: String, refBarcodeLength: Int): TemplatePolicy =
    s match {
      case Regex1(ctx, minStr, maxStr) =>
        ctx match {
          case Regex2(p1, b1, gap, p2, b2) =>
            if ((b1.length + b2.length) != refBarcodeLength) {
              throw new IllegalArgumentException(s"$s is not compatible with the provided reference file")
            }
            val min = Option(minStr).map(_.toInt)
            val max = Option(maxStr).map(_.tail.toInt)
            SplitBarcodePolicy(p1.toUpperCase, b1.length, gap.length, p2.toUpperCase, b2.length, min, max)
          case _ =>
            val km = KeyMask(ctx)
            if (km.keyLengthInBases != refBarcodeLength) {
              throw new IllegalArgumentException(s"$s is not compatible with the provided reference file")
            }
            val min = Option(minStr).map(_.toInt)
            val max = Option(maxStr).map(_.tail.toInt)
            GeneralTemplatePolicy(km, min, max)
        }
      case _ =>
        throw new IllegalArgumentException(s"Incomprehensible template barcode policy: $s")
    }

  /*
    A.................Adenine
    C.................Cytosine
    G.................Guanine
    T (or U)..........Thymine (or Uracil)
    R.................A or G
    Y.................C or T
    S.................G or C
    W.................A or T
    K.................G or T
    M.................A or C
    B.................C or G or T
    D.................A or G or T
    H.................A or C or T
    V.................A or C or G
    N.................any base
    . or -............gap
   */
  final def compatible(p: Char, b: Char): Boolean =
    (p: @switch) match {
      case 'N' => true
      case 'A' => b == 'A'
      case 'C' => b == 'C'
      case 'G' => b == 'G'
      case 'T' => b == 'T'
      case 'R' => b == 'A' || b == 'G'
      case 'Y' => b == 'C' || b == 'T'
      case 'S' => b == 'G' || b == 'C'
      case 'W' => b == 'A' || b == 'T'
      case 'K' => b == 'G' || b == 'T'
      case 'M' => b == 'A' || b == 'C'
      case 'B' => b == 'C' || b == 'G' || b == 'T'
      case 'D' => b == 'A' || b == 'G' || b == 'T'
      case 'H' => b == 'A' || b == 'C' || b == 'T'
      case 'V' => b == 'A' || b == 'C' || b == 'G'
      case _   => false
    }

  final def satisfies(template: Array[Char], seq: String, seqOffset: Int): Boolean = {
    var i = 0
    while (i < template.length) {
      if (!compatible(template(i), seq(seqOffset + i))) return false
      i += 1
    }
    true
  }

}

final case class GeneralTemplatePolicy(template: KeyMask, minStartPos: Option[Int], maxStartPos: Option[Int] = None)
    extends TemplatePolicy {

  private[this] val minStartPosInt: Int = minStartPos.getOrElse(0)
  private[this] val maxStartPosInt: Int = maxStartPos.getOrElse(Int.MaxValue)
  private[this] val firstKeyBaseOffset: Int = template.keyRanges.head.start0

  // commonly-accessed parts of the keymask
  private[this] val templateChars: Array[Char] = template.pattern.toUpperCase.toCharArray
  private[this] val contextLength: Int = template.contextLength
  private[this] val keyLength: Int = template.keyLengthInBases

  // the publicly exposed length is the keyLength
  override val length: Int = keyLength

  override def find(read: Read): Option[FoundBarcode] = {
    // loop through the sequence looking for a valid context seq
    val maxPos = math.min(read.seq.length - contextLength, maxStartPosInt)

    @tailrec
    def find(i: Int): Option[Int] =
      if (i > maxPos) None
      else if (TemplatePolicy.satisfies(templateChars, read.seq, i)) Some(i)
      else find(i + 1)

    find(minStartPosInt).map(i => extract(read, i))
  }

  private[barcode] def extract(read: Read, i: Int): FoundBarcode = {
    val keyBuf = Array.ofDim[Char](keyLength)
    var offset = 0
    template.keyRanges.foreach { kr =>
      read.seq.getChars(kr.start0 + i, kr.start0 + i + kr.length, keyBuf, offset)
      offset += kr.length
    }
    FoundBarcode(keyBuf, firstKeyBaseOffset + i)
  }

}

final case class SplitBarcodePolicy(
  prefix1: String,
  b1Length: Int,
  gap: Int,
  prefix2: String,
  b2Length: Int,
  minPrefix1StartPos: Option[Int],
  maxPrefix1StartPos: Option[Int] = None
) extends TemplatePolicy {
  import SplitBarcodePolicy.{indexOf, matches}

  private[this] val minPrefix1StartPosInt: Int = minPrefix1StartPos.getOrElse(0)
  private[this] val maxPrefix1StartPosInt: Int = maxPrefix1StartPos.getOrElse(Int.MaxValue)
  private[this] val p1Length = prefix1.length
  private[this] val p2Length = prefix2.length
  private[this] val expectedP2Offset = prefix1.length + b1Length + gap
  private[this] val patternLength = expectedP2Offset + p2Length + b2Length

  override def length: Int = b1Length + b2Length

  override def find(read: Read): Option[FoundBarcode] = {
    @tailrec
    def loop(start: Int): Option[FoundBarcode] = {
      val e = math.min(maxPrefix1StartPosInt, read.seq.length - patternLength)
      if (start > e) None
      else {
        indexOf(prefix1, read.seq, start, e) match {
          case None => None
          case Some(p1Index) =>
            val p2Index = p1Index + expectedP2Offset
            if (matches(prefix2, read.seq, p2Index)) {
              val dest = Array.ofDim[Char](length)
              // copy in the the barcodes
              read.seq.getChars(p1Index + p1Length, p1Index + p1Length + b1Length, dest, 0)
              read.seq.getChars(p2Index + p2Length, p2Index + p2Length + b2Length, dest, b1Length)
              Some(FoundBarcode(dest, p1Index + p1Length))
            } else {
              loop(p1Index + 1)
            }
        }
      }
    }
    loop(minPrefix1StartPosInt)
  }

}

object SplitBarcodePolicy {

  // assumes ASCII (1-byte) chars
  // haystack is the string we are searching
  // needle is the string we are searching for
  // start is the first place in `haystack` we will look for `needle`
  // end is the last place in `haystack` where `needle` may _begin_
  final private[barcode] def indexOf(needle: String, haystack: String, start: Int, end: Int): Option[Int] = {
    @tailrec
    def loop(i: Int): Option[Int] =
      if (i > math.min(haystack.length - needle.length, end)) {
        None
      } else {
        if (matches(needle, haystack, i)) Some(i)
        else loop(i + 1)
      }
    loop(start)
  }

  final private def matches(needle: String, haystack: String, haystackOffset: Int): Boolean = {
    @tailrec
    def loop(i: Int): Boolean =
      if (i >= needle.length) true
      else {
        if (needle(i) != haystack(haystackOffset + i)) false
        else loop(i + 1)
      }
    loop(0)
  }

}
