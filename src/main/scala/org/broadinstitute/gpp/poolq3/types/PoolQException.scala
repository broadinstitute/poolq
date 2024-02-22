/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.types

/** General exception raised by PoolQ */
final case class PoolQException(msg: String) extends RuntimeException(msg)
