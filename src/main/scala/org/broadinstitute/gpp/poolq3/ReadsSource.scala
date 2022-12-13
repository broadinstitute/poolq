/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3

import java.nio.file.Path

import cats.data.{NonEmptyList => Nel}

sealed trait ReadsSource extends Product with Serializable

object ReadsSource {

  final case class SelfContained(paths: Nel[Path]) extends ReadsSource
  final case class Split(index: Nel[Path], forward: Nel[Path]) extends ReadsSource
  final case class PairedEnd(index: Nel[Path], forward: Nel[Path], reverse: Nel[Path]) extends ReadsSource

}
