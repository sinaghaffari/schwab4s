lazy val scala3_5_0 = "3.5.0"
lazy val supportedScalaVersions = Vector(scala3_5_0)

ThisBuild / version := "0.1.0"
ThisBuild / organization := "gg.sina"
ThisBuild / scalaVersion := scala3_5_0
ThisBuild / crossScalaVersions := supportedScalaVersions
ThisBuild / versionScheme := Some("early-semver")

githubTokenSource := TokenSource.GitConfig("tokens.sbt")
githubOwner := "sinaghaffari"
githubRepository := "schwab4s"

lazy val root = (project in file("."))
  .settings(
    name := "schwab4s",
  )

libraryDependencies += "org.playframework" %% "play-ahc-ws-standalone" % "3.1.0-M2"
libraryDependencies += "org.playframework" %% "play-ws-standalone-json" % "3.1.0-M2"
libraryDependencies += "org.playframework" %% "play-json" % "3.1.0-M1"

libraryDependencies += "org.apache.pekko" %% "pekko-actor-typed" % "1.1.0"

libraryDependencies += "joda-time" % "joda-time" % "2.12.7"
libraryDependencies += "com.beachape" %% "enumeratum" % "1.7.4"
libraryDependencies += "com.beachape" %% "enumeratum-play-json" % "1.8.1"

libraryDependencies += "gg.sina" %% "monadic-simplifier" % "0.1.0"
libraryDependencies += "gg.sina" %% "monadic-simplifier-play-json" % "0.1.0"
libraryDependencies += "gg.sina" %% "agent" % "0.1.1"
