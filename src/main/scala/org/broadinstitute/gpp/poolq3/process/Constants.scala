/*
 * Copyright (c) 2026 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.process

object Constants:

  // the amount of time we're willing to wait on a queue offer or take before checking for an exception
  // we care more about throughput than latency so we don't need to check that aggressively
  val QueueTimeoutMillis: Long = 250L
