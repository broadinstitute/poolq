/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.reports

import java.io.PrintWriter
import java.nio.file.Path

import scala.util.{Try, Using}

import org.broadinstitute.gpp.poolq3.reference.Reference

// TODO: this is not currently used
object ReferenceCollisionWriter {

  def write(file: Path, reference: Reference, compat: Boolean): Try[Unit] =
    Using(new PrintWriter(file.toFile)) { writer =>
      writer.println("Construct barcodes matching multiple construct IDs:")
      reference.allBarcodes
        .map(bc => (bc, reference.idsForBarcode(bc)))
        .filter { case (_, ids) => ids.lengthCompare(1) > 1 }
        .foreach { case (bc, ids) =>
          if compat then writer.println(s"$bc:\t${ids.mkString("\t")}")
          else writer.println(s"$bc\t${ids.mkString(",")}")
        }
    }

}
