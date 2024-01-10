/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.nio.file.Path

import scala.collection.mutable

import org.broadinstitute.gpp.poolq3.barcode.FoundBarcode
import org.broadinstitute.gpp.poolq3.types.Read

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

abstract class DmuxedIterable extends CloseableIterable[Read] {

  /** `Some(barcode)` or else `None` if unmatched */
  // hack: this is sort of an encapsulation violation because ordinarily the
  // iterable is not supposed to know about barcodes, but the demultiplexed
  // case inherently crosses those lines and defining this here avoids recomputing
  // the same value potentially millions of times in a row
  def indexBarcode: Option[FoundBarcode]
}

object DmuxedIterable {

  def apply(iterable: Iterable[(Option[String], Path)], parserFor: Path => CloseableIterator[Read]): DmuxedIterable =
    new DmuxedIterableImpl(iterable, parserFor)

  def apply(data: List[(Option[String], List[String])]): DmuxedIterable = {
    val data2: List[(Option[String], List[Read])] = data.map { case (bco, seqs) =>
      (bco, seqs.zipWithIndex.map { case (seq, i) => Read(i.toString, seq) })
    }
    DmuxedIterable.forReads(data2)
  }

  def forReads(data: List[(Option[String], List[Read])]): DmuxedIterable =
    new DmuxedIterableImpl(data, CloseableIterator.ofList)

  private class DmuxedIterableImpl[A](src: Iterable[(Option[String], A)], makeIterator: A => CloseableIterator[Read])
      extends DmuxedIterable {

    private val queue: mutable.Queue[(Option[String], A)] = mutable.Queue.from(src)

    var current: CloseableIterator[Read] = _

    var indexBarcode: Option[FoundBarcode] = _

    override def iterator: CloseableIterator[Read] = new CloseableIterator[Read] {

      override def hasNext: Boolean = {
        var currentHasNext = if current == null then false else current.hasNext
        while !currentHasNext && queue.nonEmpty do {
          val head = queue.dequeue()
          if head != null then {
            val old = current
            indexBarcode = head._1.map(bc => FoundBarcode(bc.toCharArray, 0))
            current = makeIterator(head._2)
            if old != null then {
              old.close()
            }
            currentHasNext = current.hasNext
          }
        }
        currentHasNext
      }

      override def next(): Read =
        if current == null then throw new NoSuchElementException
        else current.next()

      override def close(): Unit = {
        Option(current).foreach(_.close())
      }

    }

  }

}
