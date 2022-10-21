/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

final class KnuthMorrisPratt(word: String) {
  private[this] val f: Array[Int] = KnuthMorrisPratt.failure(word)

  final def search(text: String): Option[Int] = search(text, 0, text.length)

  /** Returns `Some(idx)` where `idx` is the index of the first occurrence of `word` within the provided `text` between
    * `fromIndex` and `toIndex`, or `None` if `word` is not found within the range of `text`.
    */
  final def search(text: String, fromIndex: Int, toIndex: Int): Option[Int] = {
    val wordLength = word.length
    var m = fromIndex
    var i = 0
    while ((m + i) < toIndex) {
      if (word.charAt(i) == text.charAt(m + i)) {
        if (i == (wordLength - 1)) return Some(m)
        i += 1
      } else {
        val fi = f(i)
        if (fi > -1) {
          m = m + i - fi
          i = fi
        } else {
          m = m + i + 1
          i = 0
        }
      }
    }
    None
  }

}

object KnuthMorrisPratt {

  /** Computes the KMP failure function, which maps integers: {1,2,...,m} -> {0,1,...,m-1} such that f(q) = max { k : k
    * < q and Pk is a suffix of Pq } CLR calls this the prefix function. See CLR 1ed p. 871 for details.
    */
  def failure(word: String): Array[Int] = {
    // initialize the KMP failure array
    val f = Array.fill(word.length)(0)
    f(0) = -1
    f(1) = 0

    var wi = 2 // the word index
    var si = 0 // the substring index

    while (wi < word.length) {
      if (word(wi - 1) == word(si)) {
        f(wi) = si + 1
        si = si + 1
        wi = wi + 1
      } else if (si > 0) {
        si = f(si)
      } else {
        f(wi) = 0
        wi = wi + 1
      }
    }

    // return f
    f
  }

}
