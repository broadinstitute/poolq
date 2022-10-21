/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3

package object collection {

  implicit class ZipWithIndex1[A](private val t: Iterator[A]) extends AnyVal {
    def zipWithIndex1: Iterator[(A, Int)] = t.zipWithIndex.map { case (x, i) => (x, i + 1) }
  }

}
