/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.types

sealed trait ReadsFileType extends Product with Serializable {
  def displayName: String
}

case object FastqType extends ReadsFileType {
  override val displayName: String = "FASTQ"
}

case object SamType extends ReadsFileType {
  override val displayName: String = "SAM"
}

case object BamType extends ReadsFileType {
  override val displayName: String = "BAM"
}

case object TextType extends ReadsFileType {
  override val displayName: String = "text"
}

object ReadsFileType {

  def fromFilename(n: String): Option[ReadsFileType] =
    if n.endsWith(".fastq") || n.endsWith(".fastq.gz") then Some(FastqType)
    else if n.endsWith(".sam") then Some(SamType)
    else if n.endsWith(".bam") then Some(BamType)
    else if n.endsWith(".txt") || n.endsWith(".txt.gz") then Some(TextType)
    else None

}
