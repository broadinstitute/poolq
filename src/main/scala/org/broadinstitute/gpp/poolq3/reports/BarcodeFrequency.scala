/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

private[reports] case class BarcodeFrequency(bc: String, frequency: Int)

private[reports] object BarcodeFrequency:

  implicit val ord: Ordering[BarcodeFrequency] =
    Ordering.by[BarcodeFrequency, (Int, String)](b => (-b.frequency, b.bc)).reverse
