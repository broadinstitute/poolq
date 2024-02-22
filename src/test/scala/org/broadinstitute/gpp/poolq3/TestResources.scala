/*
 * Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.broadinstitute.gpp.poolq3

import java.nio.file.{Path, Paths}

trait TestResources:

  def resourcePath(clazz: Class[?], name: String): Path =
    val r = clazz.getResource(name)
    require(r != null, s"Can't find resource at path $name for ${getClass.getName}")
    Paths.get(r.getPath)

  def resourcePath(name: String): Path =
    val r = this.getClass.getResource(name)
    require(r != null, s"Can't find resource at path $name for ${getClass.getName}")
    Paths.get(r.getPath)

end TestResources
