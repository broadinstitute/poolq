/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

final class ConflictingBarcodeException(barcode: String, id: String, witness: String)
    extends Exception(s"Conflicting barcodes: $barcode ($id) conflicts with existing barcode $witness")
