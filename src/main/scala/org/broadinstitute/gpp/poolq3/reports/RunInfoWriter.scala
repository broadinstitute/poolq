/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.PrintWriter
import java.nio.file.Path

import scala.util.{Try, Using}

import org.broadinstitute.gpp.poolq3.{BuildInfo, PoolQConfig}

object RunInfoWriter:

  def write(file: Path, config: PoolQConfig): Try[Unit] =
    Using(new PrintWriter(file.toFile)) { writer =>
      writer.println(s"PoolQ version: ${BuildInfo.version}")
      writer.println("PoolQ command-line settings:")
      val settings = PoolQConfig.synthesizeArgs(config)
      val cli =
        settings
          .map {
            case (arg, "")    => s"  --$arg"
            case (arg, param) => s"  --$arg $param"
          }
          .mkString(" \\\n")
      writer.println(cli)
      writer.println()
    }

end RunInfoWriter
