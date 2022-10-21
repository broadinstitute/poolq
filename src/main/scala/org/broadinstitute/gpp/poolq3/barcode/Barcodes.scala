/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

final case class Barcodes(
  row: Option[FoundBarcode],
  revRow: Option[FoundBarcode],
  col: Option[FoundBarcode],
  umi: Option[FoundBarcode]
)
