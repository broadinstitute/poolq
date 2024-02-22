/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

sealed trait ReportsDialect extends Product with Serializable
case object PoolQ2Dialect extends ReportsDialect
case object PoolQ3Dialect extends ReportsDialect
case object GctDialect extends ReportsDialect
