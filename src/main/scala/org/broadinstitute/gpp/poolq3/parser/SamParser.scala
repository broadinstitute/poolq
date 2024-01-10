/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.nio.file.Path

import htsjdk.samtools.SamReaderFactory
import org.broadinstitute.gpp.poolq3.seq.reverseComplement
import org.broadinstitute.gpp.poolq3.types.Read

final class SamParser(file: Path) extends CloseableIterable[Read] {

  private[this] val readerFactory: SamReaderFactory = SamReaderFactory.makeDefault()

  private[this] class SamIterator extends CloseableIterator[Read] {
    private[this] val samReader = readerFactory.open(file.toFile)
    private[this] val samIterator = samReader.iterator()

    final override def close(): Unit = {
      samIterator.close()
      samReader.close()
    }

    final override def next(): Read = {
      val samRecord = samIterator.next()
      val readSequence =
        if samRecord.getReadNegativeStrandFlag then reverseComplement(samRecord.getReadString)
        else samRecord.getReadString
      Read(samRecord.getReadName, readSequence)
    }

    final override def hasNext: Boolean = samIterator.hasNext
  }

  override def iterator: CloseableIterator[Read] = new SamIterator()

}
