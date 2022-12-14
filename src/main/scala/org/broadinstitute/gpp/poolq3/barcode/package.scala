/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3

import java.nio.file.Path

import scala.collection.mutable

import org.broadinstitute.gpp.poolq3.ReadsSource
import org.broadinstitute.gpp.poolq3.parser.{CloseableIterable, CloseableIterator, FastqParser, SamParser, TextParser}
import org.broadinstitute.gpp.poolq3.types.{BamType, FastqType, Read, ReadsFileType, SamType, TextType}

package object barcode {

  def barcodeSource(
    config: PoolQInput,
    rowBarcodePolicy: BarcodePolicy,
    revRowBarcodePolicyOpt: Option[BarcodePolicy],
    colBarcodePolicy: BarcodePolicy,
    umiBarcodePolicyOpt: Option[BarcodePolicy]
  ): CloseableIterable[Barcodes] =
    (config.readsSource, revRowBarcodePolicyOpt) match {
      case (ReadsSource.Split(index, forward), None) =>
        new TwoFileBarcodeSource(
          parserFor(forward.toList),
          parserFor(index.toList),
          rowBarcodePolicy,
          colBarcodePolicy,
          umiBarcodePolicyOpt,
          config.readIdCheckPolicy
        )
      case (ReadsSource.PairedEnd(index, forward, reverse), Some(revRowBarcodePolicy)) =>
        new ThreeFileBarcodeSource(
          parserFor(forward.toList),
          parserFor(reverse.toList),
          parserFor(index.toList),
          rowBarcodePolicy,
          revRowBarcodePolicy,
          colBarcodePolicy,
          umiBarcodePolicyOpt,
          config.readIdCheckPolicy
        )
      case (ReadsSource.SelfContained(paths), None) =>
        new SingleFileBarcodeSource(parserFor(paths.toList), rowBarcodePolicy, colBarcodePolicy, umiBarcodePolicyOpt)
      case _ =>
        throw new IllegalArgumentException("Incompatible reads and barcode policy settings")
    }

  def parserFor(file: Path): CloseableIterable[Read] =
    ReadsFileType.fromFilename(file.getFileName.toString) match {
      case Some(FastqType)               => new FastqParser(file)
      case Some(SamType) | Some(BamType) => new SamParser(file)
      case Some(TextType)                => new TextParser(file)
      case None                          => throw new IllegalArgumentException(s"File $file is of an unknown file type")
    }

  def parserFor(files: List[Path]): CloseableIterable[Read] =
    parserFor[Path, Read](files, p => parserFor(p).iterator)

  private[barcode] def parserFor[A, B](sources: List[A], mkIterator: A => CloseableIterator[B]): CloseableIterable[B] =
    new CloseableIterable[B] {

      override def iterator: CloseableIterator[B] = new CloseableIterator[B] {

        private val queue: mutable.Queue[A] = mutable.Queue.from(sources)

        var current: CloseableIterator[B] = _

        override def hasNext: Boolean = {
          var currentHasNext = if (current == null) false else current.hasNext
          while (!currentHasNext && queue.nonEmpty) {
            val head = queue.dequeue()
            if (head != null) {
              val old = current
              current = mkIterator(head)
              if (old != null) {
                old.close()
              }
              currentHasNext = current.hasNext
            }
          }
          currentHasNext
        }

        override def next(): B = if (current == null) throw new NoSuchElementException else current.next()

        override def close(): Unit = Option(current).foreach(_.close())

      }

    }

}
