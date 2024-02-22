/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

import java.nio.file.Path

import scala.collection.mutable

import org.broadinstitute.gpp.poolq3.parser.{
  CloseableIterable,
  CloseableIterator,
  DmuxedIterable,
  FastqParser,
  SamParser,
  TextParser
}
import org.broadinstitute.gpp.poolq3.types.{BamType, FastqType, Read, ReadsFileType, SamType, TextType}
import org.broadinstitute.gpp.poolq3.{PoolQInput, ReadsSource}

def barcodeSource(
  config: PoolQInput,
  rowBarcodePolicy: BarcodePolicy,
  revRowBarcodePolicyOpt: Option[BarcodePolicy],
  colBarcodePolicyOpt: Either[Int, BarcodePolicy],
  umiBarcodePolicyOpt: Option[BarcodePolicy]
): CloseableIterable[Barcodes] =
  (config.readsSource, revRowBarcodePolicyOpt, colBarcodePolicyOpt) match
    case (ReadsSource.Split(index, forward), None, Right(colBarcodePolicy)) =>
      new TwoFileBarcodeSource(
        parserFor(forward.toList),
        parserFor(index.toList),
        rowBarcodePolicy,
        colBarcodePolicy,
        umiBarcodePolicyOpt,
        config.readIdCheckPolicy
      )
    case (ReadsSource.PairedEnd(index, forward, reverse), Some(revRowBarcodePolicy), Right(colBarcodePolicy)) =>
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
    case (ReadsSource.SelfContained(paths), None, Right(colBarcodePolicy)) =>
      new SingleFileBarcodeSource(parserFor(paths.toList), rowBarcodePolicy, colBarcodePolicy, umiBarcodePolicyOpt)
    case (ReadsSource.Dmuxed(read1), _, Left(colBarcodeLength)) =>
      new DmuxedBarcodeSource(
        DmuxedIterable(read1.toList, parserFor(_).iterator),
        rowBarcodePolicy,
        umiBarcodePolicyOpt,
        colBarcodeLength
      )
    case (ReadsSource.DmuxedPairedEnd(read1, read2), Some(revRowBarcodePolicy), Left(colBarcodeLength)) =>
      new DmuxedPairedEndBarcodeSource(
        DmuxedIterable(read1.toList, parserFor(_).iterator),
        DmuxedIterable(read2.toList, parserFor(_).iterator),
        rowBarcodePolicy,
        revRowBarcodePolicy,
        umiBarcodePolicyOpt,
        config.readIdCheckPolicy,
        colBarcodeLength
      )
    case _ =>
      throw new IllegalArgumentException("Incompatible reads and barcode policy settings")

def parserFor(file: Path): CloseableIterable[Read] =
  ReadsFileType.fromFilename(file.getFileName.toString) match
    case Some(FastqType)               => new FastqParser(file)
    case Some(SamType) | Some(BamType) => new SamParser(file)
    case Some(TextType)                => new TextParser(file)
    case None                          => throw new IllegalArgumentException(s"File $file is of an unknown file type")

def parserFor(files: List[Path]): CloseableIterable[Read] =
  parserFor[Path, Read](files, p => parserFor(p).iterator)

private[barcode] def parserFor[A, B](sources: List[A], mkIterator: A => CloseableIterator[B]): CloseableIterable[B] =
  new CloseableIterable[B]:

    override def iterator: CloseableIterator[B] = new CloseableIterator[B]:

      private val queue: mutable.Queue[A] = mutable.Queue.from(sources)

      var current: CloseableIterator[B] = _

      override def hasNext: Boolean =
        var currentHasNext = if current == null then false else current.hasNext
        while !currentHasNext && queue.nonEmpty do
          val head = queue.dequeue()
          if head != null then
            val old = current
            current = mkIterator(head)
            if old != null then old.close()
            currentHasNext = current.hasNext
        currentHasNext

      end hasNext

      override def next(): B = if current == null then throw new NoSuchElementException else current.next()

      override def close(): Unit = Option(current).foreach(_.close())
