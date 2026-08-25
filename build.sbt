ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.4"

val http4sVersion = "0.23.36"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core"     % "1.0.0-RC12",
  "org.xerial"   %  "sqlite-jdbc"     % "3.53.2.1",

  "org.typelevel" %% "cats-effect" % "3.7.1",
  "org.typelevel" %% "cats-core" % "2.13.0",

  "org.slf4j" % "slf4j-simple" % "2.0.18",

  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl"          % http4sVersion,
  "org.http4s" %% "http4s-circe"        % http4sVersion,

)

lazy val root = (project in file("."))
  .settings(
    name := "MarketWatcher"
  )
