/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3

import java.nio.file.{Path => JPath}

import scala.io.Source
import scala.util.Using

import cats.effect.Resource
import fs2.io.file.Files

package object testutil {

  def contents(p: JPath): String = Using.resource(Source.fromFile(p.toFile))(_.mkString)

  def tempFile[F[_]: Files](prefix: String, suffix: String): Resource[F, JPath] =
    Files[F].tempFile(None, prefix, suffix, None).map(_.toNioPath)

}
