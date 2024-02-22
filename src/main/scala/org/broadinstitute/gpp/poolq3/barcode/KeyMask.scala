/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import scala.annotation.tailrec

sealed abstract class KeyMask(val pattern: String, val keyRanges: Seq[KeyRange]):
  // these must be in sorted order
  assert(keyRanges.sorted == keyRanges)

  /** the number of bases encoded into the actual index key, as defined by the keyRanges */
  val keyLengthInBases: Int = keyRanges.map(_.length).sum

  /** size of the entire contiguous context sequence */
  val contextLength: Int = pattern.length

  override def toString: String = pattern

end KeyMask

object KeyMask:

  def apply(pattern: String): KeyMask =
    create(pattern, parsePatternRanges(pattern))

  def apply(contextLength: Int, keyRanges: Seq[KeyRange]): KeyMask =
    require(keyRanges.nonEmpty, "Key mask must have at least one key range")
    keyRanges.foreach { r =>
      require(r.end0 < contextLength, s"contextLength ($contextLength) is not large enough to contain key range: $r")
    }
    val mergedRanges = mergeAdjacent(keyRanges.sorted)
    val pat: String = constructPattern(contextLength, keyRanges)
    create(pat, mergedRanges)

  end apply

  private[this] def create(pattern: String, mergedRanges: Seq[KeyRange]): KeyMask =
    mergedRanges.length match
      case 1 =>
        val r = mergedRanges.head
        if r.start0 == 0 && r.length == pattern.length then KeyMask0(pattern)
        else KeyMask1(pattern, r)
      case 2 =>
        KeyMask2(pattern, mergedRanges(0), mergedRanges(1))
      case _ =>
        KeyMaskN(pattern, mergedRanges)

  def fromString(contextLength: Int, str: String): KeyMask =
    val ranges = str.split(",", -1).view.map(s => KeyRange.apply(s.trim))
    require(ranges.nonEmpty, s"KeyMask range string yields no valid ranges: '$str'")
    apply(contextLength, ranges.toIndexedSeq)

  /** Given a sorted sequence of potentially-overlapping key ranges (which represent closed intervals in the key space),
    * merges adjacent/overlapping ranges. For example, [1-9], [9-10], [12-14] should be merged to just [1-10], [12-14]
    */
  private[barcode] def mergeAdjacent(bases: Seq[KeyRange]): Seq[KeyRange] =
    // use List for pattern matching & fast seq construction
    def merge(acc: List[KeyRange], current: KeyRange): List[KeyRange] =
      acc match
        case Nil => current :: Nil
        case head :: tail =>
          if head.end0 >= current.start0 - 1 then KeyRange(head.start0, current.end0) :: tail
          else current :: acc
    // but use an IndexedSeq for efficiency later
    bases.foldLeft(List[KeyRange]())(merge).toIndexedSeq.reverse

  end mergeAdjacent

  /** Given a pattern string representing the key mask, generates the list of key ranges that are used to construct a
    * key that will be stored in the index. The input description uses a capital letters to indicate that a base at that
    * position is indexed, and lower-case letters for non-indexed bases. Restrictions on the base at any given position
    * can be represented by a non-"N" (or "n") at that position. For instance, placing an "A" at position 10 indicates
    * that all indexed context sequences contain an "A" at that position (others are skipped during indexing, and are
    * simply not present in this index).
    */
  private[barcode] def parsePatternRanges(pattern: String): List[KeyRange] =
    @tailrec
    def loop(acc: List[KeyRange], ps: List[(Char, Int)]): List[KeyRange] =
      ps match
        case Nil => acc.reverse
        case (base, startIdx) :: _ if base.isUpper =>
          val (span, rest) = ps.span { case (p, _) => p.isUpper }
          loop(KeyRange(startIdx, startIdx + span.length - 1) :: acc, rest)
        case (_, _) :: tl => loop(acc, tl)
    // in a micro-benchmark, empirically calling zipWithIndex before toList is faster than
    // the other way around
    loop(Nil, pattern.zipWithIndex.toList)

  end parsePatternRanges

  def constructPattern(length: Int, ranges: Seq[KeyRange]): String =
    val chars = Array.fill[Char](length)('n')
    ranges.foreach(range => (range.start0 to range.end0).foreach(i => chars(i) = chars(i).toUpper))
    new String(chars)

end KeyMask

//--------------------------------------------------------------------------------------------------
// KeyMask implementations
//--------------------------------------------------------------------------------------------------

/** Simplest possible `KeyMask`: no trimming or gaps, the context *is* the key */
final case class KeyMask0(override val pattern: String) extends KeyMask(pattern, List(KeyRange(0, pattern.length - 1)))

/** A simple `KeyMask` with no gaps, but with peripheral trimming of the context */
final case class KeyMask1(override val pattern: String, keyRange: KeyRange) extends KeyMask(pattern, List(keyRange))

/** A `KeyMask` with a single gap and edge-trimming, probably the most complex that will ever be needed in real world
  * situations
  */
final case class KeyMask2(override val pattern: String, keyRange1: KeyRange, keyRange2: KeyRange)
    extends KeyMask(pattern, List(keyRange1, keyRange2))

/** A `KeyMask` with a any number of gaps.  I doubt anyone will ever need this */
final case class KeyMaskN(override val pattern: String, override val keyRanges: Seq[KeyRange])
    extends KeyMask(pattern, keyRanges):
  require(keyRanges.size > 2, s"KeyMaskN must have at least 3 keyRanges: $keyRanges")
