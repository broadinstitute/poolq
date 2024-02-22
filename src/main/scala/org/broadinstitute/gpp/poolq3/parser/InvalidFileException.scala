/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.nio.file.Path

final case class InvalidFileException(file: Path, msg: Option[String] = None)
    extends RuntimeException(s"$file was invalid" + msg.fold("")(txt => s": $txt"))

object InvalidFileException:
  def apply(file: Path, msg: String): InvalidFileException = new InvalidFileException(file, Option(msg))
