/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.barcode

final case class ReadTooShortException(read: String, startPos0: Int, minLength: Int)
    extends RuntimeException(s"""Read too short: read="$read", startPos0=$startPos0, minLength=$minLength""")
