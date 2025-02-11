/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.nio.file.Path

import org.broadinstitute.gpp.poolq3.types.Read

class TextParser(file: Path) extends CloseableIterable[Read]:

  private[parser] class TextIterator(is: InputStream) extends CloseableIterator[Read]:
    private val reader = new BufferedReader(new InputStreamReader(is))
    private var lineNo: Int = 1
    private var line = reader.readLine()

    final override def next(): Read =
      if line == null then throw new NoSuchElementException
      else
        val ret = Read(s"Line $lineNo", line)
        line = reader.readLine()
        lineNo += 1
        ret

    final override def hasNext: Boolean = line != null

    final override def close(): Unit = reader.close()

  end TextIterator

  override def iterator: CloseableIterator[Read] = new TextIterator(inputStream(file))

end TextParser
