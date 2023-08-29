/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.parser

import java.io.{BufferedReader, FileInputStream, InputStreamReader}
import java.nio.file.Path
import java.util.stream.Collectors

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try, Using}

import cats.syntax.all._
import org.apache.commons.io.ByteOrderMark
import org.apache.commons.io.input.BOMInputStream

class BarcodeSet(val barcodes: Set[String]) {

  def barcodeLength: Int = barcodes.head.length

  def isDefined(s: String): Boolean = barcodes.contains(s)

}

object BarcodeSet {

  private[this] val BarcodeRe = s"""([ACGT]+)""".r

  def parseBarcode(makeException: String => Exception)(line: String): Try[String] =
    line match {
      case BarcodeRe(bc) => Success(bc)
      case _             => Failure(makeException(line))
    }

  def apply(file: Path): BarcodeSet =
    Using.resource(new FileInputStream(file.toFile)) { fin =>
      val in = BOMInputStream
        .builder()
        .setInputStream(fin)
        .setByteOrderMarks(ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE)
        .setInclude(false)
        .get()
      val br = new BufferedReader(new InputStreamReader(in))
      skipHeader(br, BarcodeRe)
      br.lines()
        .collect(Collectors.toList[String])
        .asScala
        .toList
        .map(_.trim)
        .traverse(parseBarcode(s => InvalidFileException(file, s"Invalid DNA barcode '$s'")))
        .map(bcs => new BarcodeSet(bcs.toSet))
        .flatTap(checkSet(file, _))
        .get // throws if an error was encountered
    }

  def checkSet(file: Path, barcodeSet: BarcodeSet): Try[Unit] =
    if (barcodeSet.barcodes.isEmpty) Failure(InvalidFileException(file, s"Empty barcode file"))
    else {
      val expectedLength = barcodeSet.barcodeLength
      barcodeSet.barcodes.find(_.length != expectedLength) match {
        case None => Success(())
        case Some(b) =>
          Failure(InvalidFileException(file, s"Barcode '$b' did not match expected length $expectedLength"))
      }
    }

}
