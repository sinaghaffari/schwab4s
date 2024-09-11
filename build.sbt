lazy val scala3_5_0 = "3.5.0"
lazy val supportedScalaVersions = Vector(scala3_5_0)

ThisBuild / version := "1.0.0"
ThisBuild / organization := "gg.sina"
ThisBuild / scalaVersion := scala3_5_0
ThisBuild / crossScalaVersions := supportedScalaVersions
ThisBuild / versionScheme := Some("early-semver")

githubTokenSource := TokenSource.GitConfig("tokens.sbt")
githubOwner := "sinaghaffari"
githubRepository := "schwab4s"

scalacOptions ++= Seq(
  "-Xmax-inlines", "64"
)

lazy val root = (project in file("."))
  .settings(
    name := "schwab4s",
  )

libraryDependencies += "joda-time" % "joda-time" % "2.12.7"

libraryDependencies += "dev.zio" %% "zio" % "2.1.9"
libraryDependencies += "dev.zio" %% "zio-connect-file" % "0.4.4"
libraryDependencies += "dev.zio" %% "zio-json" % "0.7.3"
libraryDependencies += "dev.zio" %% "zio-http" % "3.0.0"
libraryDependencies += "dev.zio" %% "zio-config" % "4.0.2"
libraryDependencies += "dev.zio" %% "zio-config-typesafe" % "4.0.2"
libraryDependencies += "dev.zio" %% "zio-config-magnolia" % "4.0.2"
