/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.io.{BufferedInputStream, BufferedReader, FileInputStream, InputStream}
import java.nio.file.Path
import java.util.zip.GZIPInputStream

import scala.util.matching.Regex

/** Attempts to guess whether a file is gzipped */
def isGzipped(file: Path): Boolean = file.toFile.getName.toLowerCase.endsWith(".gz")

/** Returns an appropriate `InputStream` for the file; if the file appears to be gzipped, it will return a
  * GZIPInputStream that decompresses the data on the fly.
  */
def inputStream(file: Path): InputStream = {
  val rawStream = new FileInputStream(file.toFile)
  val bufferedStream = new BufferedInputStream(rawStream)
  if (isGzipped(file)) new GZIPInputStream(bufferedStream, 8192)
  else bufferedStream
}

private[parser] def skipHeader(br: BufferedReader, re: Regex): Unit = {
  br.mark(1024)
  val line = br.readLine()
  line match {
    case re(_) => br.reset()
    case _     => // do nothing
  }
}
