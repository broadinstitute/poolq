/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.process

import java.io.{BufferedWriter, Closeable, OutputStreamWriter}
import java.nio.file.{Files, Path}

import scala.util.control.NonFatal

import org.broadinstitute.gpp.poolq3.process.UnexpectedSequenceTracker.nameFor
import org.broadinstitute.gpp.poolq3.reference.Reference
import org.log4s.{Logger, getLogger}

final class UnexpectedSequenceTracker(cacheDir: Path, colReference: Reference) extends Closeable {

  private[this] val log: Logger = getLogger

  // prep the directory and create file writers
  private[this] val outputFileWriters: Map[String, BufferedWriter] = {
    val _ = Files.createDirectories(cacheDir)
    colReference.allBarcodes.map(barcode => barcode -> newWriterFor(barcode)).toMap
  }

  def reportUnexpected(barcodes: (Array[Char], Array[Char])): Unit = {
    val (rowBarcode, columnBarcode) = barcodes

    val rowBc = new String(rowBarcode)
    val colBc = new String(columnBarcode)

    val writer = outputFileWriters(colBc)
    writer.write(rowBc)
    writer.write("\n")
  }

  override def close(): Unit =
    outputFileWriters.values.foreach { writer =>
      try {
        writer.close()
      } catch {
        case NonFatal(e) => log.error(e)("Error closing file")
      }
    }

  private[this] def newWriterFor(shard: String): BufferedWriter = {
    val name = cacheDir.resolve(nameFor(shard))
    new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(name)))
  }

}

object UnexpectedSequenceTracker {

  private[this] val fileExtension: String = ".txt"

  def nameFor(shard: String) = s"unexpected-$shard$fileExtension"

}
