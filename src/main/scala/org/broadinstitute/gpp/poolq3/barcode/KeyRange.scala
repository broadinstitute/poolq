/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

final case class KeyRange(start0: Int, end0: Int) {
  require(start0 <= end0, s"`start` must be <= `end`, got [$start0, $end0]")
  require(start0 >= 0, s"negative indices not allowed in KeyRange: [$start0, $end0]")
  require(end0 - start0 >= 0, s"KeyRange($start0, $end0) has illegal length ${end0 - start0 + 1} (must be positive)")

  def length: Int = end0 - start0 + 1

  def start1: Int = start0 + 1

  def end1: Int = end0 + 1

  override def toString: String = if length == 1 then start1.toString else s"$start1..$end1"

}

object KeyRange {

  implicit val ord: Ordering[KeyRange] = Ordering.by(kr => (kr.start0, kr.end0))

  private[this] val Range1Re = """^(\d+)$""".r
  private[this] val Range2Re = """^(\d+)(?:-|\.\.)(\d+)$""".r

  def apply(str: String): KeyRange =
    str match {
      case Range2Re(s, e) => KeyRange(s.toInt - 1, e.toInt - 1)
      case Range1Re(s)    => KeyRange(s.toInt - 1, s.toInt - 1)
      case _              => throw new IllegalArgumentException(s"Unrecognized key range `$str`")
    }

}
