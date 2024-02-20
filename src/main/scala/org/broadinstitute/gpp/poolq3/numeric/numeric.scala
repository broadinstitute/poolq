/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.numeric

import java.text.DecimalFormat

val Decimal000Format: DecimalFormat = new DecimalFormat("0.000")

val Decimal00Format: DecimalFormat = new DecimalFormat("0.00")

val Log2: Double = math.log(2)

val OneMillion: Double = 1000000.0

def percent(num: Int, denom: Int): Double = if denom == 0 then 0.0 else num * 100.0 / denom

def log2(x: Double): Double = math.log(x) / Log2

def logNormalize(num: Int, denom: Int): Double =
  if denom == 0 then 0
  else math.log1p(num * OneMillion / denom) / Log2
