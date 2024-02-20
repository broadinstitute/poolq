/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.types

import org.broadinstitute.gpp.poolq3.process.State

case class PoolQRunSummary(totalReads: Int, matchingReads: Int, matchPercent: Float, state: State)
