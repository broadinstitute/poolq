/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.types

final case class UncoordinatedReadsException(id1: String, id2: String)
    extends RuntimeException(s"Read ID $id1 did not match read ID $id2")
