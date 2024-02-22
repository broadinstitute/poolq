/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.seq

import scala.collection.mutable

val Bases: Seq[Char] = Vector('A', 'C', 'G', 'T')

val Complements: Map[Char, Char] =
  Map('A' -> 'T', 'C' -> 'G', 'G' -> 'C', 'T' -> 'A', 'N' -> 'N')

final def complement(seq: String): String =
  val length = seq.length

  val bldr = new mutable.StringBuilder()
  var i = 0
  while i < length do
    bldr.append(Complements(seq.charAt(i)))
    i += 1
  bldr.toString()

end complement

final def reverseComplement(seq: String): String =
  val bldr = new mutable.StringBuilder()

  var i = seq.length - 1
  while i > -1 do
    bldr.append(Complements(seq.charAt(i)))
    i -= 1
  bldr.toString()

end reverseComplement

/** Returns {{true}} iff the provided string consists only of DNA bases plus N */
final def isDna(seq: String): Boolean =
  var i = seq.length - 1
  while i > -1 do
    val b = seq.charAt(i)
    if b != 'A' && b != 'C' && b != 'G' && b != 'T' && b != 'N' then return false
    i -= 1
  true

/** Returns {{true}} if the provided string consists only of [ACGTactg;:-] */
final def isReferenceBarcode(seq: String): Boolean =
  if seq.isEmpty then false
  else {
    var i = seq.length - 1
    while i > -1 do {
      val b = seq.charAt(i)
        // format: off
        if (b != 'A' && b != 'a' &&
            b != 'C' && b != 'c' &&
            b != 'G' && b != 'g' &&
            b != 'T' && b != 't' &&
            b != ':' && b != ';' && b != '-') return false
        // format: on
      i -= 1
    }
    true
  }

/** Returns the number of Ns in a given DNA sequence */
final def nCount(seq: String): Int =
  var n = 0
  var i = seq.length - 1
  while i > -1 do
    if seq.charAt(i) == 'N' then n += 1
    i -= 1
  n

final val NoN: Int = -1
final val PolyN: Int = -2

// yes this is terrible and I hate it
// if seq contains no Ns, returns -1
// if seq contains precisely 1 N, returns the index of the N
// if seq contains more than 1 N, returns -2
final def singleNIndex(seq: Array[Char]): Int =
  var n = NoN
  var i = seq.length - 1
  while i > -1 do
    if seq(i) == 'N' then
      if n > -1 then return PolyN
      else n = i
    i -= 1
  n

end singleNIndex

/** Returns the number of Ns in a given DNA sequence up to a maximum */
final def nCount(seq: String, max: Int): Int =
  var n = 0
  var i = seq.length - 1
  while i > -1 do
    if seq.charAt(i) == 'N' then n += 1
    if n > 0 && n >= max then return n
    i -= 1
  n

end nCount

/** Returns the number of Ns in a give DNA sequence */
final def nCount(seq: Array[Char]): Int =
  var i = seq.length - 1
  var n = 0
  while i > -1 do
    if seq(i) == 'N' then n += 1
    i -= 1
  n

/** Returns the number of Ns in a give DNA sequence up to a maximum */
final def nCount(seq: Array[Char], max: Int): Int =
  var i = seq.length - 1
  var n = 0
  while i > -1 do
    if seq(i) == 'N' then n += 1
    if n > 0 && n >= max then return n
    i -= 1
  n

end nCount

/** Returns true if the provided DNA sequence contains an N */
final def containsN(seq: Array[Char]): Boolean =
  var i = seq.length - 1
  while i > -1 do
    if seq(i) == 'N' then return true
    i -= 1
  false

final def containsN(seq: String): Boolean =
  var i = seq.length - 1
  while i > -1 do
    if seq.charAt(i) == 'N' then return true
    i -= 1
  false

final def countMismatches(s1: CharSequence, s2: CharSequence): Int =
  require(s1.length == s2.length, "Strings must be of the same length")
  var distance = 0
  for i <- 0 until s1.length do if s1.charAt(i) != s2.charAt(i) then distance += 1
  distance
