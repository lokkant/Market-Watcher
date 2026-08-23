ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.4"

libraryDependencies += "org.typelevel" %% "cats-effect" % "3.7.0"
libraryDependencies += "org.typelevel" %% "cats-core" % "2.13.0"

lazy val root = (project in file("."))
  .settings(
    name := "MarketWatcher"
  )
