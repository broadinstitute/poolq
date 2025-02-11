/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.nio.file.Path

import htsjdk.samtools.SamReaderFactory
import org.broadinstitute.gpp.poolq3.seq.reverseComplement
import org.broadinstitute.gpp.poolq3.types.Read

final class SamParser(file: Path) extends CloseableIterable[Read]:

  private val readerFactory: SamReaderFactory = SamReaderFactory.makeDefault()

  private class SamIterator extends CloseableIterator[Read]:
    private val samReader = readerFactory.open(file.toFile)
    private val samIterator = samReader.iterator()

    final override def close(): Unit =
      samIterator.close()
      samReader.close()

    final override def next(): Read =
      val samRecord = samIterator.next()
      val readSequence =
        if samRecord.getReadNegativeStrandFlag then reverseComplement(samRecord.getReadString)
        else samRecord.getReadString
      Read(samRecord.getReadName, readSequence)

    final override def hasNext: Boolean = samIterator.hasNext

  end SamIterator

  override def iterator: CloseableIterator[Read] = new SamIterator()

end SamParser
