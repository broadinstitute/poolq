/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.process

import java.io.Closeable

import org.broadinstitute.gpp.poolq3.barcode.Barcodes

trait Consumer extends Closeable:

  def start(): Unit

  def close(): Unit

  def consume(parsedBarcode: Barcodes): Unit

  def readsProcessed: Int

  def matchingReads: Int

  def matchPercent: Float

  def state: State

end Consumer
