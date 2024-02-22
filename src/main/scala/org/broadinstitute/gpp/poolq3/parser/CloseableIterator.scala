/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.io.Closeable

abstract class CloseableIterator[A] extends Iterator[A] with Closeable

object CloseableIterator:

  /** A convenience implementation used for testing */
  def ofList[A](xs: List[A]): CloseableIterator[A] = new CloseableIterator[A]:
    val iter = xs.iterator
    override def close(): Unit = ()
    override def hasNext: Boolean = iter.hasNext
    override def next(): A = iter.next()

end CloseableIterator
