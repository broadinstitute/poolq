/*
 * Copyright (c) 2022 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.types

trait OutputFileType extends Product with Serializable
case object CountsFileType extends OutputFileType
case object QualityFileType extends OutputFileType
case object ConditionBarcodeCountsSummaryFileType extends OutputFileType
case object LogNormalizedCountsFileType extends OutputFileType
case object BarcodeCountsFileType extends OutputFileType
case object CorrelationFileType extends OutputFileType
case object RunInfoFileType extends OutputFileType
case object UnexpectedSequencesFileType extends OutputFileType
