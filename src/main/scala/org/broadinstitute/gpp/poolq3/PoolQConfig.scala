/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3

import java.io.File
import java.nio.file.{Files, Path, Paths}

import scala.collection.mutable

import cats.data.{NonEmptyList => Nel}
import cats.syntax.all._
import org.broadinstitute.gpp.poolq3.PoolQConfig.DefaultPath
import org.broadinstitute.gpp.poolq3.reports.{GctDialect, PoolQ2Dialect, PoolQ3Dialect, ReportsDialect}
import org.broadinstitute.gpp.poolq3.types.{PoolQException, ReadIdCheckPolicy}
import scopt.{OptionParser, Read}

final case class PoolQInput(
  rowReference: Path = DefaultPath,
  colReference: Path = DefaultPath,
  umiReference: Option[Path] = None,
  globalReference: Option[Path] = None,
  rowReads: Option[Path] = None,
  reverseRowReads: Option[Path] = None,
  colReads: Option[Path] = None,
  reads: Option[Path] = None,
  readIdCheckPolicy: ReadIdCheckPolicy = ReadIdCheckPolicy.Strict,
  // these are companion to rowReads, reverseRowReads, colReads, and reads
  // they are added thusly to retain source compatibility with the old object
  addlRowReads: List[Path] = Nil,
  addlReverseRowReads: List[Path] = Nil,
  addlColReads: List[Path] = Nil,
  addlReads: List[Path] = Nil
) {

  def readsSourceE: Either[Exception, ReadsSource] = (rowReads, reverseRowReads, colReads, reads) match {
    case (None, None, None, Some(r)) => Right(ReadsSource.SelfContained(Nel(r, addlReads)))
    case (Some(rr), None, Some(cr), None) =>
      val rs = ReadsSource.Split(Nel(cr, addlColReads), Nel(rr, addlRowReads))
      if (rs.forward.length == rs.index.length) Right(rs)
      else Left(PoolQException("Number of row, column, and reverse reads files must match"))
    case (Some(rr), Some(rrr), Some(cr), None) =>
      val rs = ReadsSource.PairedEnd(Nel(cr, addlColReads), Nel(rr, addlRowReads), Nel(rrr, addlReverseRowReads))
      if (rs.forward.length == rs.index.length && rs.forward.length == rs.reverse.length) Right(rs)
      else Left(PoolQException("Number of row and column reads files must match"))
    case _ => Left(PoolQException("Conflicting input options"))
  }

  def readsSource: ReadsSource = readsSourceE.fold(e => throw e, rs => rs)

}

final case class PoolQOutput(
  countsFile: Path = Paths.get("counts.txt"),
  normalizedCountsFile: Path = Paths.get("lognormalized-counts.txt"),
  barcodeCountsFile: Path = Paths.get("barcode-counts.txt"),
  qualityFile: Path = Paths.get("quality.txt"),
  correlationFile: Path = Paths.get("correlation.txt"),
  unexpectedSequencesFile: Path = Paths.get("unexpected-sequences.txt"),
  umiQualityFile: Path = Paths.get("umi-quality.txt"),
  umiCountsFilesDir: Option[Path] = None,
  umiBarcodeCountsFilesDir: Option[Path] = None,
  runInfoFile: Path = Paths.get("runinfo.txt")
)

final case class PoolQConfig(
  input: PoolQInput = PoolQInput(),
  output: PoolQOutput = PoolQOutput(),
  rowMatchFn: String = "mismatch",
  colMatchFn: String = "exact",
  countAmbiguous: Boolean = false,
  rowBarcodePolicyStr: String = "",
  reverseRowBarcodePolicyStr: Option[String] = None,
  colBarcodePolicyStr: String = "",
  umiBarcodePolicyStr: Option[String] = None,
  skipUnexpectedSequenceReport: Boolean = false,
  unexpectedSequenceCacheDir: Option[Path] = None,
  removeUnexpectedSequenceCache: Boolean = true,
  unexpectedSequencesToReport: Int = 100,
  skipShortReads: Boolean = false,
  reportsDialect: ReportsDialect = PoolQ3Dialect,
  alwaysCountColumnBarcodes: Boolean = false,
  noopConsumer: Boolean = false
) {

  def isPairedEnd =
    reverseRowBarcodePolicyStr.isDefined &&
      (input.readsSourceE match {
        case Right(ReadsSource.PairedEnd(_, _, _)) => true
        case _                                     => false
      })

}

object PoolQConfig {

  private[poolq3] val DefaultPath = Paths.get(".")

  implicit private[this] val readPath: Read[Path] = implicitly[Read[File]].map(_.toPath)

  implicit private[this] val readPaths: Read[(Path, List[Path])] = implicitly[Read[Seq[File]]].map { files =>
    files.toList.map(_.toPath) match {
      case Nil       => throw new IllegalArgumentException(s"No argument provided")
      case (x :: xs) => (x, xs)
    }
  }

  implicit private[this] val readReadIdCheckPolicy: Read[ReadIdCheckPolicy] =
    implicitly[Read[String]].map(ReadIdCheckPolicy.forName)

  def parse(args: Array[String]): Option[PoolQConfig] = {

    val parser: OptionParser[PoolQConfig] = new OptionParser[PoolQConfig]("poolq") {
      private[this] def existsAndIsReadable(f: Path): Either[String, Unit] =
        if (!Files.exists(f)) failure(s"Could not find ${f.toAbsolutePath}")
        else if (!Files.isReadable(f)) failure(s"Could not read ${f.toAbsolutePath}")
        else success

      head(BuildInfo.name, BuildInfo.version)

      opt[Path]("row-reference")
        .valueName("<file>")
        .required()
        .action((f, c) => c.copy(input = c.input.copy(rowReference = f)))
        .text("reference file for row barcodes (i.e., constructs)")
        .validate(existsAndIsReadable)

      opt[Path]("col-reference")
        .valueName("<file>")
        .required()
        .action((f, c) => c.copy(input = c.input.copy(colReference = f)))
        .text("reference file for column barcodes (i.e., conditions)")
        .validate(existsAndIsReadable)

      opt[File]("umi-reference").valueName("<file>").action { (f, c) =>
        c.copy(input = c.input.copy(umiReference = Some(f.toPath)))
      }

      opt[File]("global-reference").valueName("<file>").action { (f, c) =>
        c.copy(input = c.input.copy(globalReference = Some(f.toPath)))
      }

      opt[(Path, List[Path])]("row-reads")
        .valueName("<files>")
        .action { case ((p, ps), c) => c.copy(input = c.input.copy(rowReads = Some(p), addlRowReads = ps)) }
        .text("required if reads are split between two files")
        .validate { case (p, ps) => (p :: ps).traverse_(existsAndIsReadable) }

      opt[(Path, List[Path])]("rev-row-reads")
        .valueName("<files>")
        .action { case ((p, ps), c) =>
          c.copy(input = c.input.copy(reverseRowReads = Some(p), addlReverseRowReads = ps))
        }
        .text("required for processing paired-end sequencing data")
        .validate { case (p, ps) => (p :: ps).traverse_(existsAndIsReadable) }

      opt[(Path, List[Path])]("col-reads")
        .valueName("<files>")
        .action { case ((p, ps), c) => c.copy(input = c.input.copy(colReads = Some(p), addlColReads = ps)) }
        .text("required if reads are split between two files")
        .validate { case (p, ps) => (p :: ps).traverse_(existsAndIsReadable) }

      opt[(Path, List[Path])]("reads")
        .valueName("<files>")
        .action { case ((p, ps), c) => c.copy(input = c.input.copy(reads = Some(p), addlReads = ps)) }
        .text("required if reads are contained in a single file")
        .validate { case (p, ps) => (p :: ps).traverse_(existsAndIsReadable) }

      opt[ReadIdCheckPolicy]("read-id-check-policy")
        .valueName("<policy>")
        .action((p, c) => c.copy(input = c.input.copy(readIdCheckPolicy = p)))
        .text("read ID check policy; one of [lax, strict, illumina]")

      opt[String]("row-matcher")
        .valueName("<matcher>")
        .action((m, c) => c.copy(rowMatchFn = m))
        .text("function used to match row barcodes against the row reference database")

      opt[String]("col-matcher")
        .valueName("<matcher>")
        .action((m, c) => c.copy(colMatchFn = m))
        .text("function used to match column barcodes against the column reference database")

      opt[Boolean]("count-ambiguous")
        .action((b, c) => c.copy(countAmbiguous = b))
        .text("when true, counts ambiguous fuzzy matches for all potential row barcodes")

      opt[String]("row-barcode-policy").valueName("<barcode-policy>").action { (p, c) =>
        c.copy(rowBarcodePolicyStr = p)
      }

      opt[String]("rev-row-barcode-policy").valueName("<barcode-policy>").action { (p, c) =>
        c.copy(reverseRowBarcodePolicyStr = Some(p))
      }

      opt[String]("col-barcode-policy").valueName("<barcode-policy>").action { (p, c) =>
        c.copy(colBarcodePolicyStr = p)
      }

      opt[String]("umi-barcode-policy").valueName("<barcode-policy>").action { (p, c) =>
        c.copy(umiBarcodePolicyStr = Some(p))
      }

      opt[Path]("umi-counts-dir").valueName("<file>").action { (f, c) =>
        c.copy(output = c.output.copy(umiCountsFilesDir = Some(f)))
      }

      opt[Path]("umi-barcode-counts-dir").valueName("<file>").action { (f, c) =>
        c.copy(output = c.output.copy(umiBarcodeCountsFilesDir = Some(f)))
      }

      opt[Path]("quality").valueName("<file>").action((f, c) => c.copy(output = c.output.copy(qualityFile = f)))

      opt[Path]("counts").valueName("<file>").action((f, c) => c.copy(output = c.output.copy(countsFile = f)))

      opt[Path]("normalized-counts").valueName("<file>").action { (f, c) =>
        c.copy(output = c.output.copy(normalizedCountsFile = f))
      }

      opt[Path]("barcode-counts").valueName("<file>").action { (f, c) =>
        c.copy(output = c.output.copy(barcodeCountsFile = f))
      }

      opt[Path]("scores").valueName("<file>").action((f, c) => c.copy(output = c.output.copy(countsFile = f)))

      opt[Path]("normalized-scores").valueName("<file>").action { (f, c) =>
        c.copy(output = c.output.copy(normalizedCountsFile = f))
      }

      opt[Path]("barcode-scores").valueName("<file>").action { (f, c) =>
        c.copy(output = c.output.copy(barcodeCountsFile = f))
      }

      opt[Path]("correlation").valueName("<file>").action { (f, c) =>
        c.copy(output = c.output.copy(correlationFile = f))
      }

      opt[Path]("run-info").valueName("<file>").action((f, c) => c.copy(output = c.output.copy(runInfoFile = f)))

      opt[Int]("unexpected-sequence-threshold")
        .valueName("<number>")
        .action((n, c) => c.copy(unexpectedSequencesToReport = n))
        .validate { n =>
          if (n > 0) success
          else failure(s"Unexpected sequence threshold must be greater than 0, got: $n")
        }

      opt[Path]("unexpected-sequences").valueName("<file>").action { (f, c) =>
        c.copy(output = c.output.copy(unexpectedSequencesFile = f))
      }

      opt[Path]("umi-quality").valueName("<file>").action { (f, c) =>
        c.copy(output = c.output.copy(umiQualityFile = f))
      }

      opt[Path]("unexpected-sequence-cache").valueName("<cache-dir>").action { (f, c) =>
        c.copy(unexpectedSequenceCacheDir = Some(f))
      }

      opt[Unit]("retain-unexpected-sequence-cache").hidden().action { (_, c) =>
        c.copy(removeUnexpectedSequenceCache = false)
      }

      opt[Unit]("skip-unexpected-sequence-report").action((_, c) => c.copy(skipUnexpectedSequenceReport = true))

      opt[Unit]("skip-short-reads").action((_, c) => c.copy(skipShortReads = true))

      opt[Unit]("always-count-col-barcodes")
        .action((_, c) => c.copy(alwaysCountColumnBarcodes = true))
        .text("Count each column barcode regardless of whether a row barcode was found in the read")

      opt[Unit]("compat")
        .action((_, c) => c.copy(reportsDialect = PoolQ2Dialect))
        .text("Enable PoolQ 2.X compatibility mode")

      opt[Unit]("gct").action((_, c) => c.copy(reportsDialect = GctDialect)).text("Output counts in GCT format")

      // this is used for throughput testing
      opt[Unit]("noop").hidden().action((_, c) => c.copy(noopConsumer = true))

      checkConfig { c =>
        val readsCheck = (c.input.reads, c.input.rowReads, c.input.colReads) match {
          case (None, None, None) => failure("No reads files specified.")
          case (None, None, Some(_)) =>
            failure("Column barcode file specified but no row barcodes file specified.")
          case (None, Some(_), None) =>
            failure("Row barcode file specified but no column barcodes file specified.")
          case _ => success
        }

        val pairedEndConsistencyCheck = (c.input.reverseRowReads, c.reverseRowBarcodePolicyStr) match {
          case (Some(_), None) => failure("Reverse row reads file specified but no reverse barcode policy specified")
          case (None, Some(_)) => failure("Reverse barcode policy specified but now reverse row reads file specified")
          case _               => success
        }

        readsCheck >> pairedEndConsistencyCheck
      }
    }

    parser.parse(args, PoolQConfig())

  }

  def synthesizeArgs(config: PoolQConfig): List[(String, String)] = {
    val args = new mutable.ArrayBuffer[(String, String)]

    // umi
    val umiInfo = (config.input.umiReference, config.umiBarcodePolicyStr).tupled

    // input files
    val input = config.input
    args += (("row-reference", input.rowReference.getFileName.toString))
    args += (("col-reference", input.colReference.getFileName.toString))
    umiInfo.map(_._1).foreach(file => args += (("umi-reference", file.getFileName.toString)))
    input.globalReference.foreach(file => args += (("global-reference", file.getFileName.toString)))
    input.rowReads.foreach(file => args += (("row-reads", file.getFileName.toString)))
    input.reverseRowReads.foreach(file => args += (("rev-row-reads", file.getFileName.toString)))
    input.colReads.foreach(file => args += (("col-reads", file.getFileName.toString)))
    input.reads.foreach(file => args += (("reads", file.getFileName.toString)))
    args += (("read-id-check-policy", input.readIdCheckPolicy.name))

    // run control
    args += (("row-matcher", config.rowMatchFn))
    args += (("col-matcher", config.colMatchFn))
    if (config.countAmbiguous) {
      args += (("count-ambiguous", ""))
    }
    args += (("row-barcode-policy", config.rowBarcodePolicyStr))
    config.reverseRowBarcodePolicyStr.foreach(p => args += (("rev-row-barcode-policy", p)))
    args += (("col-barcode-policy", config.colBarcodePolicyStr))
    umiInfo.map(_._2).foreach(str => args += (("umi-barcode-policy", str)))

    // deal with the unexpected sequence options
    if (config.skipUnexpectedSequenceReport) {
      args += (("skip-unexpected-sequence-report", ""))
    } else {
      // give whatever path we were given here - this _may_ not need to be included at all, honestly
      config.unexpectedSequenceCacheDir.foreach(file => args += (("unexpected-sequence-cache", file.toString)))
      args += (("unexpected-sequence-threshold", config.unexpectedSequencesToReport.toString))
    }

    if (config.skipShortReads) {
      args += (("skip-short-reads", ""))
    }
    if (config.reportsDialect == PoolQ2Dialect) {
      args += (("compat", ""))
    }

    // output files
    val output = config.output
    args += (("counts", output.countsFile.getFileName.toString))
    args += (("normalized-counts", output.normalizedCountsFile.getFileName.toString))
    args += (("barcode-counts", output.barcodeCountsFile.getFileName.toString))
    args += (("quality", output.qualityFile.getFileName.toString))
    args += (("correlation", output.correlationFile.getFileName.toString))
    umiInfo.foreach { _ =>
      args += (("umi-quality", output.umiQualityFile.getFileName.toString))
      output.umiCountsFilesDir.foreach(d => args += (("umi-counts-dir", d.toString)))
      output.umiBarcodeCountsFilesDir.foreach(d => args += (("umi-barcode-counts-dir", d.toString)))
    }
    if (!config.skipUnexpectedSequenceReport) {
      args += (("unexpected-sequences", output.unexpectedSequencesFile.getFileName.toString))
    }
    if (config.alwaysCountColumnBarcodes) {
      args += (("always-count-col-barcodes", ""))
    }
    args += (("run-info", output.runInfoFile.getFileName.toString))

    args.toList
  }

}
