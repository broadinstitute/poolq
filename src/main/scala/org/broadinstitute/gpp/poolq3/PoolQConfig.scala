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
  rowReads: Option[(Option[String], Path)] = None,
  reverseRowReads: Option[(Option[String], Path)] = None,
  colReads: Option[Path] = None,
  reads: Option[(Option[String], Path)] = None,
  readIdCheckPolicy: ReadIdCheckPolicy = ReadIdCheckPolicy.Strict,
  // these are companion to rowReads, reverseRowReads, colReads, and reads
  // they are added thusly to retain source compatibility with the old object
  addlRowReads: List[(Option[String], Path)] = Nil,
  addlReverseRowReads: List[(Option[String], Path)] = Nil,
  addlColReads: List[Path] = Nil,
  addlReads: List[(Option[String], Path)] = Nil,
  demultiplexed: Boolean = false
) {

  def readsSourceE: Either[Exception, ReadsSource] = (rowReads, reverseRowReads, colReads, reads, demultiplexed) match {
    case (None, None, None, Some(r), false) =>
      Right(ReadsSource.SelfContained(Nel(r._2, addlReads.view.map(_._2).toList)))

    case (Some(rr), None, Some(cr), None, false) =>
      val rs = ReadsSource.Split(Nel(cr, addlColReads), Nel(rr._2, addlRowReads.view.map(_._2).toList))
      if (rs.forward.length == rs.index.length) Right(rs)
      else Left(PoolQException("Number of row, column, and reverse reads files must match"))

    case (Some(rr), Some(rrr), Some(cr), None, false) =>
      val rs = ReadsSource.PairedEnd(
        Nel(cr, addlColReads),
        Nel(rr._2, addlRowReads.view.map(_._2).toList),
        Nel(rrr._2, addlReverseRowReads.view.map(_._2).toList)
      )
      if (rs.forward.length == rs.index.length && rs.forward.length == rs.reverse.length) Right(rs)
      else Left(PoolQException("Number of row and column reads files must match"))

    case (Some(rr), None, None, None, true) =>
      Right(ReadsSource.Dmuxed(Nel(rr, addlRowReads)))

    case (Some(rr), Some(rrr), None, None, true) =>
      val rs = ReadsSource.DmuxedPairedEnd(Nel(rr, addlRowReads), Nel(rrr, addlReverseRowReads))
      if (rs.read1.map(_._1) == rs.read2.map(_._1)) Right(rs)
      else Left(PoolQException("Row and column reads files must match"))

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
  colBarcodePolicyStr: Option[String] = None,
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

  private[poolq3] val BarcodePathRegex = "([ACGT]+):(.+)".r

  private[poolq3] val DefaultPath = Paths.get(".")

  implicit private[this] val readPath: Read[Path] = implicitly[Read[File]].map(_.toPath)

  implicit private[this] val readPaths: Read[(Path, List[Path])] = implicitly[Read[Seq[File]]].map { files =>
    files.toList.map(_.toPath) match {
      case Nil       => throw new IllegalArgumentException(s"No argument provided")
      case (x :: xs) => (x, xs)
    }
  }

  implicit private[this] val readBarcodePaths: Read[List[(Option[String], Path)]] = implicitly[Read[Seq[String]]].map {
    args =>
      args.view.map { arg =>
        arg match {
          case BarcodePathRegex(bc, pathStr) => (Option(bc), Paths.get(pathStr))
          case _                             => (None, Paths.get(arg))
        }
      }.toList
  }

  implicit private[this] val readReadIdCheckPolicy: Read[ReadIdCheckPolicy] =
    implicitly[Read[String]].map(ReadIdCheckPolicy.forName)

  def parse(args: Array[String]): Option[PoolQConfig] = {

    val parser: OptionParser[PoolQConfig] = new OptionParser[PoolQConfig]("poolq") {
      private[this] def existsAndIsReadable(f: Path): Either[String, Unit] =
        if (!Files.exists(f)) failure(s"Could not find ${f.toAbsolutePath}")
        else if (!Files.isReadable(f)) failure(s"Could not read ${f.toAbsolutePath}")
        else success

      locally {
        val _ = head(BuildInfo.name, BuildInfo.version)

        val _ = opt[Path]("row-reference")
          .valueName("<file>")
          .required()
          .action((f, c) => c.copy(input = c.input.copy(rowReference = f)))
          .text("reference file for row barcodes (i.e., constructs)")
          .validate(existsAndIsReadable)

        val _ = opt[Path]("col-reference")
          .valueName("<file>")
          .required()
          .action((f, c) => c.copy(input = c.input.copy(colReference = f)))
          .text("reference file for column barcodes (i.e., conditions)")
          .validate(existsAndIsReadable)

        val _ = opt[File]("umi-reference").valueName("<file>").action { (f, c) =>
          c.copy(input = c.input.copy(umiReference = Some(f.toPath)))
        }

        val _ = opt[File]("global-reference").valueName("<file>").action { (f, c) =>
          c.copy(input = c.input.copy(globalReference = Some(f.toPath)))
        }

        val _ = opt[List[(Option[String], Path)]]("row-reads")
          .valueName("<files>")
          .action { case (ps, c) => c.copy(input = c.input.copy(rowReads = ps.headOption, addlRowReads = ps.drop(1))) }
          .text("required if reads are split between two files")
          .validate(_.view.map(_._2).toList.traverse_(existsAndIsReadable))

        val _ = opt[List[(Option[String], Path)]]("rev-row-reads")
          .valueName("<files>")
          .action { case (ps, c) =>
            c.copy(input = c.input.copy(reverseRowReads = ps.headOption, addlReverseRowReads = ps.drop(1)))
          }
          .text("required for processing paired-end sequencing data")
          .validate(_.view.map(_._2).toList.traverse_(existsAndIsReadable))

        val _ = opt[(Path, List[Path])]("col-reads")
          .valueName("<files>")
          .action { case ((p, ps), c) => c.copy(input = c.input.copy(colReads = Some(p), addlColReads = ps)) }
          .text("required if reads are split between two files")
          .validate { case (p, ps) => (p :: ps).traverse_(existsAndIsReadable) }

        val _ = opt[List[(Option[String], Path)]]("reads")
          .valueName("<files>")
          .action { case (ps, c) => c.copy(input = c.input.copy(reads = ps.headOption, addlReads = ps.drop(1))) }
          .text("required if reads are contained in a single file")
          .validate(_.view.map(_._2).toList.traverse_(existsAndIsReadable))

        val _ = opt[ReadIdCheckPolicy]("read-id-check-policy")
          .valueName("<policy>")
          .action((p, c) => c.copy(input = c.input.copy(readIdCheckPolicy = p)))
          .text("read ID check policy; one of [lax, strict, illumina]")

        val _ = opt[Unit]("demultiplexed")
          .action((_, c) => c.copy(input = c.input.copy(demultiplexed = true)))
          .text("when true, expects demultiplexed FASTQ files")

        val _ = opt[String]("row-matcher")
          .valueName("<matcher>")
          .action((m, c) => c.copy(rowMatchFn = m))
          .text("function used to match row barcodes against the row reference database")

        val _ = opt[String]("col-matcher")
          .valueName("<matcher>")
          .action((m, c) => c.copy(colMatchFn = m))
          .text("function used to match column barcodes against the column reference database")

        val _ = opt[Boolean]("count-ambiguous")
          .action((b, c) => c.copy(countAmbiguous = b))
          .text("when true, counts ambiguous fuzzy matches for all potential row barcodes")

        val _ = opt[String]("row-barcode-policy").valueName("<barcode-policy>").action { (p, c) =>
          c.copy(rowBarcodePolicyStr = p)
        }

        val _ = opt[String]("rev-row-barcode-policy").valueName("<barcode-policy>").action { (p, c) =>
          c.copy(reverseRowBarcodePolicyStr = Some(p))
        }

        val _ = opt[String]("col-barcode-policy").valueName("<barcode-policy>").action { (p, c) =>
          c.copy(colBarcodePolicyStr = Some(p))
        }

        val _ = opt[String]("umi-barcode-policy").valueName("<barcode-policy>").action { (p, c) =>
          c.copy(umiBarcodePolicyStr = Some(p))
        }

        val _ = opt[Path]("umi-counts-dir").valueName("<file>").action { (f, c) =>
          c.copy(output = c.output.copy(umiCountsFilesDir = Some(f)))
        }

        val _ = opt[Path]("umi-barcode-counts-dir").valueName("<file>").action { (f, c) =>
          c.copy(output = c.output.copy(umiBarcodeCountsFilesDir = Some(f)))
        }

        val _ =
          opt[Path]("quality").valueName("<file>").action((f, c) => c.copy(output = c.output.copy(qualityFile = f)))

        val _ = opt[Path]("counts").valueName("<file>").action((f, c) => c.copy(output = c.output.copy(countsFile = f)))

        val _ = opt[Path]("normalized-counts").valueName("<file>").action { (f, c) =>
          c.copy(output = c.output.copy(normalizedCountsFile = f))
        }

        val _ = opt[Path]("barcode-counts").valueName("<file>").action { (f, c) =>
          c.copy(output = c.output.copy(barcodeCountsFile = f))
        }

        val _ = opt[Path]("scores").valueName("<file>").action((f, c) => c.copy(output = c.output.copy(countsFile = f)))

        val _ = opt[Path]("normalized-scores").valueName("<file>").action { (f, c) =>
          c.copy(output = c.output.copy(normalizedCountsFile = f))
        }

        val _ = opt[Path]("barcode-scores").valueName("<file>").action { (f, c) =>
          c.copy(output = c.output.copy(barcodeCountsFile = f))
        }

        val _ = opt[Path]("correlation").valueName("<file>").action { (f, c) =>
          c.copy(output = c.output.copy(correlationFile = f))
        }

        val _ =
          opt[Path]("run-info").valueName("<file>").action((f, c) => c.copy(output = c.output.copy(runInfoFile = f)))

        val _ = opt[Int]("unexpected-sequence-threshold")
          .valueName("<number>")
          .action((n, c) => c.copy(unexpectedSequencesToReport = n))
          .validate { n =>
            if (n > 0) success
            else failure(s"Unexpected sequence threshold must be greater than 0, got: $n")
          }

        val _ = opt[Path]("unexpected-sequences").valueName("<file>").action { (f, c) =>
          c.copy(output = c.output.copy(unexpectedSequencesFile = f))
        }

        val _ = opt[Path]("umi-quality").valueName("<file>").action { (f, c) =>
          c.copy(output = c.output.copy(umiQualityFile = f))
        }

        val _ = opt[Path]("unexpected-sequence-cache").valueName("<cache-dir>").action { (f, c) =>
          c.copy(unexpectedSequenceCacheDir = Some(f))
        }

        val _ = opt[Unit]("retain-unexpected-sequence-cache").hidden().action { (_, c) =>
          c.copy(removeUnexpectedSequenceCache = false)
        }

        val _ =
          opt[Unit]("skip-unexpected-sequence-report").action((_, c) => c.copy(skipUnexpectedSequenceReport = true))

        val _ = opt[Unit]("skip-short-reads").action((_, c) => c.copy(skipShortReads = true))

        val _ = opt[Unit]("always-count-col-barcodes")
          .action((_, c) => c.copy(alwaysCountColumnBarcodes = true))
          .text("Count each column barcode regardless of whether a row barcode was found in the read")

        val _ = opt[Unit]("compat")
          .action((_, c) => c.copy(reportsDialect = PoolQ2Dialect))
          .text("Enable PoolQ 2.X compatibility mode")

        val _ =
          opt[Unit]("gct").action((_, c) => c.copy(reportsDialect = GctDialect)).text("Output counts in GCT format")

        // this is used for throughput testing
        val _ = opt[Unit]("noop").hidden().action((_, c) => c.copy(noopConsumer = true))

        val _ = checkConfig { c =>
          val readsCheck = (c.input.reads, c.input.rowReads, c.input.colReads, c.input.demultiplexed) match {
            case (None, None, None, _) => failure("No reads files specified.")
            case (None, None, Some(_), false) =>
              failure("Column barcode file specified but no row barcodes file specified.")
            case (_, _, Some(_), true) =>
              failure("Column barcode file specified for demultiplexed reads.")
            case (None, Some(_), None, false) =>
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
    }

    parser.parse(args, PoolQConfig())

  }

  def synthesizeArgs(config: PoolQConfig): List[(String, String)] = {
    val args = new mutable.ArrayBuffer[(String, String)]

    // umi
    val umiInfo = (config.input.umiReference, config.umiBarcodePolicyStr).tupled

    def files(name: String, path: Option[Path], addl: List[Path]): Option[(String, String)] =
      path.map(file => (name, (file :: addl).map(_.getFileName.toString).mkString(",")))

    def barcodeFiles(
      name: String,
      path: Option[(Option[String], Path)],
      addl: List[(Option[String], Path)]
    ): Option[(String, String)] =
      path.map { file =>
        val barcodedFiles = (file :: addl)
          .map { case (bcOpt, file) =>
            val prefix = bcOpt.fold("")(bc => s"$bc:")
            s"$prefix${file.getFileName.toString}"
          }
          .mkString(",")
        (name, barcodedFiles)
      }

    // input files
    val input = config.input
    args += (("row-reference", input.rowReference.getFileName.toString))
    args += (("col-reference", input.colReference.getFileName.toString))
    umiInfo.map(_._1).foreach(file => args += (("umi-reference", file.getFileName.toString)))
    input.globalReference.foreach(file => args += (("global-reference", file.getFileName.toString)))
    barcodeFiles("row-reads", input.rowReads, input.addlRowReads).foreach(t => args += t)
    barcodeFiles("rev-row-reads", input.reverseRowReads, input.addlReverseRowReads).foreach(t => args += t)
    files("col-reads", input.colReads, input.addlColReads).foreach(t => args += t)
    barcodeFiles("reads", input.reads, input.addlReads).foreach(t => args += t)
    args += (("read-id-check-policy", input.readIdCheckPolicy.name))

    // run control
    args += (("row-matcher", config.rowMatchFn))
    args += (("col-matcher", config.colMatchFn))
    if (config.countAmbiguous) {
      args += (("count-ambiguous", ""))
    }
    args += (("row-barcode-policy", config.rowBarcodePolicyStr))
    config.reverseRowBarcodePolicyStr.foreach(p => args += (("rev-row-barcode-policy", p)))
    config.colBarcodePolicyStr.foreach(pol => args += (("col-barcode-policy", pol)))
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
