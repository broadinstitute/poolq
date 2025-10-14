/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.PrintWriter
import java.nio.file.{Path, Paths}

import scala.collection.mutable

import org.broadinstitute.gpp.poolq3.hist.ReadOnlyHistogram
import org.broadinstitute.gpp.poolq3.reference.Reference

def writeRowIdentifiers(rowReference: Reference, rowBc: String, pw: PrintWriter): Unit =
  // write row identifiers
  val rowBarcodeIds = rowReference.idsForBarcode(rowBc).mkString(",")
  val rowInputBarcode = rowReference.referenceBarcodeForDnaBarcode(rowBc)
  pw.print(s"$rowInputBarcode\t$rowBarcodeIds\t")

def countsHeaderText(dialect: ReportsDialect, colHeadings: String, nRows: Int, nCols: Int): String =
  dialect match
    case PoolQ3Dialect => s"Row Barcode\tRow Barcode IDs\t$colHeadings"
    case PoolQ2Dialect => s"Construct Barcode\tConstruct IDs\t$colHeadings"
    case GctDialect =>
      s"""#1.2
           |$nRows\t$nCols
           |NAME\tDescription\t$colHeadings""".stripMargin

/** Returns a map from column ID to the total read count for that column */
def getColumnReadCounts(
    rowReference: Reference,
    colReference: Reference,
    hist: ReadOnlyHistogram[(String, String)]
): Map[String, Long] =
  colReference.allIds.map { colId =>
    val readCount =
      (for
        colBarcodeLong <- colReference.barcodesForId(colId)
        rowBarcodeLong <- rowReference.allBarcodes
      yield hist.count((rowBarcodeLong, colBarcodeLong))).sum

    colId -> readCount
  }.toMap

def parseFilename(p: Path): ParsedFilename =
  val nameStr = p.getFileName.toString
  val lastDotIdx = nameStr.lastIndexOf('.')
  val (base, ext) =
    if lastDotIdx == -1 then (nameStr, None)
    else
      val (b, e) = nameStr.splitAt(lastDotIdx)
      (b, Some(e))
  ParsedFilename(Option(p.getParent).getOrElse(Paths.get(".")), base, ext)

end parseFilename

/** Returns the maximum `n` `A`s in `xs`, in descending order
  * @param xs
  *   a sequence of `A`
  * @param n
  *   the number to return
  * @param ord
  *   the ordering over `A`
  * @tparam A
  *   the type of elements
  * @return
  */
def topN[A](xs: Seq[A], n: Int)(implicit ord: Ordering[A]): List[A] =
  // split xs to take the first `n` elements blindly
  val (firstN, rest) = xs.splitAt(n)

  // mutable.PriorityQueue is a max heap; we want a min heap of the largest `n` `A`'s seen so far, initialized with
  // the first `n` elements of `xs`
  val minHeap = mutable.PriorityQueue[A](firstN*)(ord.reverse)

  // for the rest, find the smallest element, call it `y`, and put the larger of `x` and `y` back in the min heap
  rest.foreach { x =>
    val y = minHeap.dequeue()
    minHeap.enqueue(ord.max(x, y))
  }
  minHeap.dequeueAll.reverse.toList

end topN
