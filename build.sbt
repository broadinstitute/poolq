val artifactId = "poolq"

inThisBuild(
  List(
    scalaVersion := "3.3.6",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    versionScheme := Some("early-semver")
  )
)

lazy val versions = new {
  val betterFiles = "3.9.2"
  val catsEffect3 = "3.6.3"
  val cats = "2.13.0"
  val commonsIo = "2.20.0"
  val commonsText = "1.14.0"
  val commonsMath3 = "3.6.1"
  val fastutil = "8.5.16"
  val fs2 = "3.12.2"
  val log4s = "1.10.0"
  val logback = "1.2.13"
  val munit = "1.2.0"
  val munitCatsEffect3 = "2.1.0"
  val munitScalaCheck = "1.2.0"
  val samTools = "3.0.5"
  val scalaCheck = "1.19.0"
  val scalaCsv = "2.0.0"
  val scalaTest = "3.2.19"
  val scalaTestPlusScalaCheck = "3.2.18.0"
  val scopt = "4.1.0"
  val slf4j = "1.7.36"
}

lazy val libraries = new {
  val betterFiles = "com.github.pathikrit" %% "better-files" % versions.betterFiles
  val cats = "org.typelevel" %% "cats-core" % versions.cats
  val catsEffect3 = "org.typelevel" %% "cats-effect" % versions.catsEffect3
  val commonsIo = "commons-io" % "commons-io" % versions.commonsIo
  val commonsMath3 = "org.apache.commons" % "commons-math3" % versions.commonsMath3
  val fastutil = "it.unimi.dsi" % "fastutil" % versions.fastutil
  val fs2Core = "co.fs2" %% "fs2-core" % versions.fs2
  val fs2Io = "co.fs2" %% "fs2-io" % versions.fs2
  val log4s = "org.log4s" %% "log4s" % versions.log4s
  val logbackCore = "ch.qos.logback" % "logback-core" % versions.logback
  val logbackClassic = "ch.qos.logback" % "logback-classic" % versions.logback
  val samtools = "com.github.samtools" % "htsjdk" % versions.samTools
  val scalaCsv = "com.github.tototoshi" %% "scala-csv" % versions.scalaCsv
  val scopt = "com.github.scopt" %% "scopt" % versions.scopt
  val slf4j = "org.slf4j" % "slf4j-api" % versions.slf4j

  // test dependency definitions
  val commonsText = "org.apache.commons" % "commons-text" % versions.commonsText
  val munit = "org.scalameta" %% "munit" % versions.munit
  val munitScalacheck = "org.scalameta" %% "munit-scalacheck" % versions.munitScalaCheck
  val munitCatsEffect = "org.typelevel" %% "munit-cats-effect" % versions.munitCatsEffect3
  val scalaTest = "org.scalatest" %% "scalatest" % versions.scalaTest
  val scalaCheck = "org.scalacheck" %% "scalacheck" % versions.scalaCheck
  val scalaTestPlusScalaCheck = "org.scalatestplus" %% "scalacheck-1-17" % versions.scalaTestPlusScalaCheck
}

lazy val dependencies =
  List(
    libraries.cats,
    libraries.commonsIo,
    libraries.commonsMath3,
    libraries.fastutil,
    libraries.log4s,
    libraries.logbackCore % Runtime,
    libraries.logbackClassic % Runtime,
    libraries.samtools,
    libraries.scalaCsv,
    libraries.scopt,
    libraries.slf4j,
    libraries.betterFiles % Test,
    libraries.catsEffect3 % Test,
    libraries.commonsText % Test,
    libraries.fs2Core % Test,
    libraries.fs2Io % Test,
    libraries.munit % Test,
    libraries.munitCatsEffect % Test,
    libraries.munitScalacheck % Test,
    libraries.scalaCheck % Test,
    libraries.scalaTest % Test,
    libraries.scalaTestPlusScalaCheck % Test
  )

lazy val headerLicenseText =
  """|Copyright (c) 2024 The Broad Institute, Inc. All rights reserved.
     |
     |SPDX-License-Identifier: BSD-3-Clause
     |""".stripMargin

lazy val headerSettings = List(
  organizationName := "The Broad Institute",
  headerLicense := Some(HeaderLicense.Custom(headerLicenseText)),
  headerEmptyLine := false
)

lazy val assemblySettings = List(
  assembly / assemblyJarName := "../bin/poolq3.jar",
  assembly / assemblyMergeStrategy := {
    case "logback.xml" => MergeStrategy.first
    case "logback-test.xml" => MergeStrategy.discard
    case PathList("module-info.class") => MergeStrategy.discard
    case PathList("META-INF", "versions", xs @ _, "module-info.class") => MergeStrategy.discard
    case "module-info.class" => MergeStrategy.first
    case x =>
      val old = (assembly / assemblyMergeStrategy).value
      old(x)
  },
  assembly / mainClass := Some("org.broadinstitute.gpp.poolq3.PoolQ")
)

lazy val publishSettings = List(
  // Publish to GitHub Packages:
  githubOwner := "broadinstitute",
  githubRepository := artifactId,
  githubTokenSource := TokenSource.Environment("GITHUB_TOKEN") || TokenSource.GitConfig("github.token")
)

lazy val poolq = project
  .in(file("."))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "poolq3",
    organization := "org.broadinstitute.gpp",
    libraryDependencies := dependencies,
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "org.broadinstitute.gpp.poolq3",
    testFrameworks += new TestFramework("munit.Framework"),
    // Tests pass in parallel, but SLF4J logging behaves weirdly. Disable this flag to examine test
    // log output; leave this enabled for very fast test execution.
    Test / parallelExecution := true
  )
  .settings(headerSettings: _*)
  .settings(assemblySettings: _*)
  .settings(publishSettings: _*)
