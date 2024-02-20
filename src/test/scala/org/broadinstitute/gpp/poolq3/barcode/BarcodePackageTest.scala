/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import munit.FunSuite
import org.broadinstitute.gpp.poolq3.parser.CloseableIterator

class BarcodePackageTest extends FunSuite:

  private class TestIterator(n: Int) extends CloseableIterator[Int]:
    private var closed = false

    private val internal = Range(0, n).iterator

    def isClosed: Boolean = closed

    override def close(): Unit = closed = true

    override def hasNext: Boolean = internal.hasNext

    override def next(): Int = internal.next()

  end TestIterator

  test("parserFor") {
    // we need to keep references to the iterators we make
    var iters: List[TestIterator] = Nil
    val iterable = parserFor[Int, Int](
      List(2, 2, 3),
      x =>
        val ret = new TestIterator(x)
        iters ::= ret
        ret
    )

    // get the whole iterator, convert it to a list, and close it (closes the last iterator)
    val aggregateIter = iterable.iterator
    val list = aggregateIter.toList
    aggregateIter.close()

    // check that we got all the data we expected _and_ that the underlying iterators were closed
    assertEquals(list, List(0, 1, 0, 1, 0, 1, 2))
    assert(iters.forall(_.isClosed))
  }

  test("parserFor (2)") {
    // we need to keep references to the iterators we make
    var iters: List[TestIterator] = Nil
    val iterable = parserFor[Int, Int](
      List(2, 2, 3),
      x =>
        val ret = new TestIterator(x)
        iters ::= ret
        ret
    )

    // get the whole iterator, convert it to a list, and close it (closes the last iterator)
    val aggregateIter = iterable.iterator
    val list = aggregateIter.take(3).toList
    aggregateIter.close()

    // check that we got all the data we expected _and_ that the underlying iterators were closed
    assertEquals(list, List(0, 1, 0))
    assert(iters.forall(_.isClosed))
  }

  test("parserFor (3)") {
    // we need to keep references to the iterators we make
    var iters: List[TestIterator] = Nil
    val iterable = parserFor[Int, Int](
      Nil,
      x =>
        val ret = new TestIterator(x)
        iters ::= ret
        ret
    )

    // get the whole iterator, convert it to a list, and close it (closes the last iterator)
    val aggregateIter = iterable.iterator
    val list = aggregateIter.take(3).toList
    aggregateIter.close()

    // check that we got all the data we expected _and_ that the underlying iterators were closed
    assertEquals(list, Nil)
    assert(iters.forall(_.isClosed))
  }

end BarcodePackageTest
