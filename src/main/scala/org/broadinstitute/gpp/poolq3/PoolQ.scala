/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3

import java.nio.file.{Files, Path}

import scala.util.{Failure, Success, Try, Using}

import cats.syntax.all.*
import org.broadinstitute.gpp.poolq3.PoolQConfig.synthesizeArgs
import org.broadinstitute.gpp.poolq3.barcode.{BarcodePolicy, Barcodes, barcodeSource}
import org.broadinstitute.gpp.poolq3.parser.{BarcodeSet, CloseableIterable, ReferenceData}
import org.broadinstitute.gpp.poolq3.process.{Consumer, NoOpConsumer, PoolQProcess, ScoringConsumer, UnexpectedSequenceTracker}
import org.broadinstitute.gpp.poolq3.reference.{ExactReference, Reference}
import org.broadinstitute.gpp.poolq3.reports.{BarcodeCountsWriter, CorrelationFileWriter, CountsWriter, LogNormalizedCountsWriter, QualityWriter, RunInfoWriter, UmiQualityWriter, UnexpectedSequenceWriter}
import org.broadinstitute.gpp.poolq3.types.{BarcodeCountsFileType, ConditionBarcodeCountsSummaryFileType, CountsFileType, LogNormalizedCountsFileType, OutputFileType, PoolQRunSummary, PoolQSummary, QualityFileType, RunInfoFileType, UnexpectedSequencesFileType}
import org.log4s.{Logger, getLogger}

object PoolQ:

  private val log: Logger = getLogger

  private val AlwaysWrittenFiles: Set[OutputFileType] =
    Set(
      CountsFileType,
      QualityFileType,
      ConditionBarcodeCountsSummaryFileType,
      LogNormalizedCountsFileType,
      BarcodeCountsFileType,
      RunInfoFileType
    )

  final def main(args: Array[String]): Unit =
    PoolQConfig.parse(args) match
      case None => System.exit(-1)
      case Some(config) =>
        run(config) match
          case Success(_) => // do nothing
          case Failure(t) =>
            log.error(t)("PoolQ failed")
            System.exit(-1)

  /** The main entry point for PoolQ3 as an API */
  final def run(config: PoolQConfig): Try[PoolQSummary] =
    log.info(s"PoolQ version: ${BuildInfo.version}")
    logCli(config)

    log.info("Reading row reference data")
    val rowReferenceData = ReferenceData(config.input.rowReference)

    log.info("Reading column reference data")
    val colReferenceData = ReferenceData(config.input.colReference).forColumnBarcodes(config.reportsDialect)

    val globalReferenceDataOpt = config.input.globalReference.map {
      log.info("Reading global reference data")
      ReferenceData(_)
    }

    val (rowBarcodePolicy, revRowBarcodePolicyOpt, rowBarcodeLength) =
      makeRowBarcodePolicy(
        rowReferenceData,
        config.rowBarcodePolicyStr,
        config.reverseRowBarcodePolicyStr,
        config.input.reverseRowReads,
        config.skipShortReads
      )

    if config.isPairedEnd then
      require(
        rowReferenceData.barcodeLength == rowBarcodeLength,
        "In paired-end mode, the length of the barcodes in the reference data must be exactly equal to the length of the barcodes " +
          s"according to the barcode policies. Reference barcodes have length ${rowReferenceData.barcodeLength}, policies require $rowBarcodeLength"
      )

    val colBarcodePolicyOpt =
      config.colBarcodePolicyStr.map(pol => BarcodePolicy(pol, colReferenceData.barcodeLength, config.skipShortReads))

    val umiInfo = (config.input.umiReference, config.umiBarcodePolicyStr).mapN { (r, p) =>
      log.info("Reading UMI reference data")
      val ref = BarcodeSet(r)
      val pol = BarcodePolicy(p, ref.barcodeLength, skipShortReads = false)
      (ref, pol)
    }

    log.info("Building row reference")
    val rowReference: Reference =
      Reference(
        config.rowMatchFn,
        ReferenceData.truncator(rowBarcodeLength),
        config.countAmbiguous,
        rowReferenceData.mappings
      )

    log.info("Building column reference")
    val colBarcodeLength = colBarcodePolicyOpt.map(_.length).getOrElse(colReferenceData.barcodeLength)
    val colReference: Reference =
      Reference(
        config.colMatchFn,
        ReferenceData.truncator(colBarcodeLength),
        config.countAmbiguous,
        colReferenceData.mappings
      )

    val globalReference = globalReferenceDataOpt.map { referenceData =>
      log.info("Building global reference")
      ExactReference(referenceData.mappings, identity, includeAmbiguous = false)
    }

    val colBarcodePolicyOrLength: Either[Int, BarcodePolicy] = colBarcodePolicyOpt.toRight(colReference.barcodeLength)

    val barcodes: CloseableIterable[Barcodes] =
      barcodeSource(config.input, rowBarcodePolicy, revRowBarcodePolicyOpt, colBarcodePolicyOrLength, umiInfo.map(_._2))

    lazy val unexpectedSequenceCacheDirOpt: Option[Path] =
      if config.skipUnexpectedSequenceReport then None
      else
        val ret = config.unexpectedSequenceCacheDir.map(Files.createDirectories(_)).orElse {
          val ret: Path = Files.createTempDirectory("unexpected-sequence-cache")
          Some(ret)
        }
        ret.foreach(path => log.info(s"Writing unexpected sequence cache files to $path"))
        ret

    lazy val unexpectedSequenceTrackerOpt: Option[UnexpectedSequenceTracker] =
      unexpectedSequenceCacheDirOpt.map(new UnexpectedSequenceTracker(_, colReference))

    val consumer =
      if config.noopConsumer then new NoOpConsumer
      else
        new ScoringConsumer(
          rowReference,
          colReference,
          config.countAmbiguous,
          config.alwaysCountColumnBarcodes,
          umiInfo.map(_._1),
          unexpectedSequenceTrackerOpt,
          config.isPairedEnd
        )

    for
      runSummary <- runProcess(barcodes, consumer)
      state = runSummary.state
      counts = state.known
      _ <- Try(log.info(s"Writing counts file ${config.output.countsFile}"))
      _ <- CountsWriter.write(
        config.output.countsFile,
        config.output.umiCountsFilesDir,
        counts,
        rowReference,
        colReference,
        umiInfo.map(_._1),
        config.reportsDialect
      )
      _ = log.info(s"Writing quality file ${config.output.qualityFile}")
      _ <- QualityWriter.write(
        config.output.qualityFile,
        config.output.conditionBarcodeCountsSummaryFile,
        state,
        rowReference,
        colReference,
        config.isPairedEnd
      )
      _ <- umiInfo.fold(().pure[Try])(_ => UmiQualityWriter.write(config.output.umiQualityFile, state))
      _ = log.info(s"Writing log-normalized counts file ${config.output.normalizedCountsFile}")
      normalizedCounts = LogNormalizedCountsWriter.logNormalizedCounts(counts, rowReference, colReference)
      _ <- LogNormalizedCountsWriter.write(
        config.output.normalizedCountsFile,
        normalizedCounts,
        rowReference,
        colReference,
        config.reportsDialect
      )
      _ = log.info(s"Writing barcode counts file ${config.output.barcodeCountsFile}")
      _ <- BarcodeCountsWriter.write(
        config.output.barcodeCountsFile,
        config.output.umiBarcodeCountsFilesDir,
        counts,
        rowReference,
        colReference,
        umiInfo.map(_._1),
        config.reportsDialect
      )
      _ = log.info(s"Writing correlation file ${config.output.correlationFile}")
      cfto <- CorrelationFileWriter.write(config.output.correlationFile, normalizedCounts, rowReference, colReference)
      usfto <- unexpectedSequenceCacheDirOpt.fold(Try(Option.empty[OutputFileType])) { dir =>
        log.info(s"Writing unexpected sequence report ${config.output.unexpectedSequencesFile}")
        val ret =
          UnexpectedSequenceWriter
            .write(
              config.output.unexpectedSequencesFile,
              dir,
              config.unexpectedSequencesToReport,
              colReference,
              globalReference,
              config.unexpectedSequenceMaxSampleSize
            )
            .as(UnexpectedSequencesFileType.some)
        if config.removeUnexpectedSequenceCache then
          log.info(s"Removing unexpected sequence cache ${config.unexpectedSequenceCacheDir}")
          UnexpectedSequenceWriter.removeCache(dir)
        ret
      }
      _ = log.info(s"Writing run info ${config.output.unexpectedSequencesFile}")
      _ <- RunInfoWriter.write(config.output.runInfoFile, config)
      _ = log.info("PoolQ complete")
    yield PoolQSummary(runSummary, AlwaysWrittenFiles ++ Set(cfto, usfto).flatten)

    end for

  end run

  def runProcess(barcodes: CloseableIterable[Barcodes], consumer: Consumer): Try[PoolQRunSummary] =
    Using(barcodes.iterator) { iterator =>
      val process = new PoolQProcess(iterator, consumer)
      process.run()
    }

  private[poolq3] def makeRowBarcodePolicy(
      rowReferenceData: ReferenceData,
      rowBarcodePolicyStr: String,
      reverseRowBarcodePolicyStr: Option[String],
      reverseRowReads: Option[(Option[String], Path)],
      skipShortReads: Boolean
  ): (BarcodePolicy, Option[BarcodePolicy], Int) =
    (reverseRowBarcodePolicyStr, reverseRowReads)
      .mapN { (revPolicy, _) =>
        val (forwardRowBcLength, revRowBcLength) = rowReferenceData.barcodeLengths
        val rowBarcodePolicy = BarcodePolicy(rowBarcodePolicyStr, forwardRowBcLength, skipShortReads)
        val revRowBarcodePolicy = BarcodePolicy(revPolicy, revRowBcLength, skipShortReads)
        (rowBarcodePolicy, Some(revRowBarcodePolicy), rowBarcodePolicy.length + revRowBarcodePolicy.length)
      }
      .getOrElse {
        val rowBarcodePolicy = BarcodePolicy(rowBarcodePolicyStr, rowReferenceData.barcodeLength, skipShortReads)
        (rowBarcodePolicy, None, rowBarcodePolicy.length)
      }

  private def logCli(config: PoolQConfig): Unit =
    val logStr =
      synthesizeArgs(config)
        .map {
          case (param, "") => s"--$param"
          case (param, arg) => s"--$param $arg"
        }
        .mkString(" \\\n")
    log.info(s"PoolQ command-line settings:\n$logStr")

  end logCli

end PoolQ
