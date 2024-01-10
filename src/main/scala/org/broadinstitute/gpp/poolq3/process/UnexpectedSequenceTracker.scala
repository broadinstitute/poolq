/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.process

import java.io.{BufferedWriter, Closeable, OutputStreamWriter}
import java.nio.file.{Files, Path}

import scala.util.control.NonFatal

import it.unimi.dsi.fastutil.objects.{Object2ObjectMap, Object2ObjectRBTreeMap}
import org.broadinstitute.gpp.poolq3.seq.Bases
import org.log4s.{Logger, getLogger}

final class UnexpectedSequenceTracker(reportDir: Path) extends Closeable {

  private[this] val log: Logger = getLogger

  private[this] val fileExtension: String = ".txt"

  private[this] val outputFileWriters: Object2ObjectMap[String, BufferedWriter] = new Object2ObjectRBTreeMap()

  // prep the directory and create file writers
  Files.createDirectories(reportDir)
  enumeratePrefixes.foreach(shard => outputFileWriters.put(shard, newWriterFor(shard)))

  def reportUnexpected(barcodes: (Array[Char], Array[Char])): Unit = {
    val (rowBarcode, columnBarcode) = barcodes

    val rowBc = new String(rowBarcode)
    val colBc = new String(columnBarcode)

    val writer = outputFileWriters(rowBc.substring(0, 4))
    writer.write(rowBc + "," + colBc + "\n")
  }

  override def close(): Unit =
    outputFileWriters.values().forEach { writer =>
      try {
        writer.close()
      } catch {
        case NonFatal(e) => log.error(e)("Error closing file")
      }
    }

  private[this] def newWriterFor(shard: String): BufferedWriter = {
    val name = reportDir.resolve(s"unexpected-$shard$fileExtension")
    new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(name)))
  }

  private[this] def enumeratePrefixes: Iterable[String] =
    for
      b1 <- Bases
      b2 <- Bases
      b3 <- Bases
      b4 <- Bases
    yield s"$b1$b2$b3$b4"

}
