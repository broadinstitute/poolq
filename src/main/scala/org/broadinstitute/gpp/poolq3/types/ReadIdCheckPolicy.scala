/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3.types

import scala.annotation.tailrec

sealed trait ReadIdCheckPolicy:
  def check(r1: Read, r2: Read): Unit
  def name: String

object ReadIdCheckPolicy:

  def forName(s: String): ReadIdCheckPolicy = s.toLowerCase() match
    case "lax"      => ReadIdCheckPolicy.Lax
    case "strict"   => ReadIdCheckPolicy.Strict
    case "illumina" => ReadIdCheckPolicy.Illumina
    case _          => throw new IllegalArgumentException(s"$s is not a read ID check policy")

  case object Lax extends ReadIdCheckPolicy:
    def check(r1: Read, r2: Read): Unit = ()
    val name: String = "lax"

  case object Strict extends ReadIdCheckPolicy:

    def check(r1: Read, r2: Read): Unit =
      if r1.id != r2.id then throw UncoordinatedReadsException(r1.id, r2.id)
      else ()

    val name: String = "strict"

  case object Illumina extends ReadIdCheckPolicy:

    // we check all the characters in the ID up to the first space we find; the pattern we are matching is:
    // @<instrument>:<run number>:<flowcell ID>:<lane>:<tile>:<x-pos>:<y-pos>:<UMI> <read>:<is filtered>:<control number>:<index>
    // for details, see
    // https://support.illumina.com/help/BaseSpace_OLH_009008/Content/Source/Informatics/BS/FileFormat_FASTQ-files_swBS.htm
    def check(r1: Read, r2: Read): Unit =
      val rid1 = r1.id
      val rid2 = r2.id
      val end = math.min(rid1.length(), rid2.length())

      @tailrec
      def loop(i: Int): Unit =
        if i >= end then ()
        else
          val c1 = rid1.charAt(i)
          val c2 = rid2.charAt(i)
          if c1 == ' ' && c2 == ' ' then ()
          else
            if c1 != c2 then throw UncoordinatedReadsException(rid1, rid2)
            loop(i + 1)

      loop(1) // first char is assumed to be `@`

    end check

    val name = "illumina"

  end Illumina

end ReadIdCheckPolicy
