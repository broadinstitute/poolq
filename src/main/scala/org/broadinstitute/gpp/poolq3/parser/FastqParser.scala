/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.nio.file.Path

import org.broadinstitute.gpp.poolq3.types.Read

final class FastqParser(file: Path) extends CloseableIterable[Read] {

  /** Wraps the underlying `InputStream`, taking ownership of it. Thus, closing this iterator closes the stream.
    */
  private[parser] class FastqIterator(is: InputStream) extends CloseableIterator[Read] {

    private[this] val reader = new BufferedReader(new InputStreamReader(is))
    private[this] var line = reader.readLine()

    final private[this] def nextLine(): String = {
      val ret = line
      line = reader.readLine()
      ret
    }

    final override def next(): Read = {
      // get the next record and make sure it's complete
      val line0 = nextLine()
      val line1 = nextLine()
      nextLine()
      val line3 = nextLine()

      if (line3 == null) {
        throw InvalidFileException(file, "File contains an incomplete FASTQ read")
      } else Read(line0, line1)
    }

    final override def hasNext: Boolean = line != null

    final override def close(): Unit = reader.close()

  }

  override def iterator: CloseableIterator[Read] = new FastqIterator(inputStream(file))

}
