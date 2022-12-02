/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3

import org.broadinstitute.gpp.poolq3.parser.ReferenceEntry

/** Provides classes implementing reference databases as well as utility functions used by the various reference
  * database implementations.
  *
  * ==Overview==
  * [[org.broadinstitute.gpp.poolq3.reference.Reference]] defines the the basic interface for a single dimension of
  * reference data.
  *
  * [[org.broadinstitute.gpp.poolq3.reference.ExactReference]] provides a basic implementation using exact matching. It
  * is suitable for use as a column reference, although more efficient implementations may be possible.
  */
package object reference {

  def referenceFor(
    matcher: String,
    barcodeProcessor: String => String,
    includeAmbiguous: Boolean,
    bs: Seq[ReferenceEntry]
  ): Reference =
    matcher.toLowerCase match {
      case "exact"    => ExactReference(bs, barcodeProcessor, includeAmbiguous)
      case "mismatch" => VariantReference(bs, barcodeProcessor, includeAmbiguous)
      case _ =>
        throw new IllegalArgumentException(
          s"Unknown matching function `$matcher`. Please choose either exact or mismatch."
        )
    }

}
