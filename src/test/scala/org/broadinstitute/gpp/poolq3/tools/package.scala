/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3

import scala.util.Random

import org.broadinstitute.gpp.poolq3.seq.nCount

package object tools {

  def timed(times: Int)(f: Unit => Unit): Long = {
    val range = 1 to times
    val t0 = System.currentTimeMillis()
    range.foreach(_ => f(()))
    val t1 = System.currentTimeMillis()
    t1 - t0
  }

  def nanoTimed[A](times: Int)(f: Unit => A): (A, Long) = {
    val range = 1 to times
    var a: A = f(())
    val t0 = System.nanoTime()
    range.foreach(_ => a = f(()))
    val t1 = System.nanoTime()
    (a, t1 - t0)
  }

  def withNs(barcode: String, n: Int): String = {
    require(n <= barcode.length)
    val bases = barcode.toCharArray
    while nCount(bases) < n do {
      bases(Random.nextInt(bases.length)) = 'N'
    }
    new String(bases)
  }

}
