/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.types

case class PoolQSummary(runSummary: PoolQRunSummary, outputFiles: Set[OutputFileType])
