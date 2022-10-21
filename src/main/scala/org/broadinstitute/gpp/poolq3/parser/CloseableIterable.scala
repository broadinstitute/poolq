/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

abstract class CloseableIterable[A] extends Iterable[A] {
  override def iterator: CloseableIterator[A]
}

object CloseableIterable {

  def ofList[A](list: List[A]): CloseableIterable[A] = new CloseableIterable[A] {

    override def iterator: CloseableIterator[A] = new CloseableIterator[A] {
      val underlying: Iterator[A] = list.iterator
      override def close(): Unit = {}
      override def hasNext: Boolean = underlying.hasNext
      override def next(): A = underlying.next()
    }

  }

}
