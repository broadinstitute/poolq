/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.process

import org.broadinstitute.gpp.poolq3.barcode.Barcodes
import org.broadinstitute.gpp.poolq3.hist.{BasicShardedHistogram, OpenHashMapHistogram, TupleHistogram}

class NoOpConsumer extends Consumer:

  var reads = 0

  final override def start(): Unit = {}

  final override def consume(parsedBarcode: Barcodes): Unit =
    reads += 1

  override def readsProcessed: Int = reads

  override def matchingReads: Int = 0

  override def matchPercent: Float = 0.0f

  final override def close(): Unit = {}

  final override val state =
    new State(
      new BasicShardedHistogram[String, (String, String)](new TupleHistogram()),
      new OpenHashMapHistogram(),
      new OpenHashMapHistogram(),
      new OpenHashMapHistogram()
    )

end NoOpConsumer
