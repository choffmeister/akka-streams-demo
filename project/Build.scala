import sbt._
import sbt.Keys._
import xerial.sbt.Pack.{ pack => sbtPack, _ }

object Build extends sbt.Build {
  lazy val dist = TaskKey[File]("dist", "Builds the distribution packages")

  lazy val buildSettings = Seq(
    scalaVersion := "2.11.5",
    scalacOptions ++= Seq("-encoding", "utf8"))

  lazy val coordinateSettings = Seq(
    organization := "de.choffmeister",
    version := "0.0.1-SNAPSHOT")

  lazy val resolverSettings = Seq(
    resolvers += "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/")

  lazy val commonSettings = Defaults.coreDefaultSettings ++ coordinateSettings ++ buildSettings ++ resolverSettings

  lazy val appPackSettings = packSettings ++ Seq(
    packMain := Map("genesis" -> "de.choffmeister.genesis.Application"),
    packExtraClasspath := Map("genesis" -> Seq("${PROG_HOME}/conf")))

  lazy val core = (project in file("genesis-core"))
    .settings(commonSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.2",
      "com.typesafe" % "config" % "1.2.0",
      "com.typesafe.akka" %% "akka-actor" % "2.3.7",
      "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-M4",
      "com.typesafe.akka" %% "akka-slf4j" % "2.3.7",
      "com.typesafe.akka" %% "akka-testkit" % "2.3.7" % "test",
      "org.specs2" %% "specs2" % "2.4.1" % "test"))
    .settings(name := "genesis-core")

  lazy val app = (project in file("genesis-app"))
    .settings(commonSettings: _*)
    .settings(appPackSettings: _*)
    .settings(name := "genesis-app")
    .dependsOn(core % "test->test;compile->compile")

  lazy val root = (project in file("."))
    .settings(coordinateSettings: _*)
    .settings(name := "coreci")
    .settings(dist <<= (target, sbtPack in app) map { (targetDir, appPack) =>
      val distDir = targetDir / "dist"
      val distBinDir = distDir / "bin"
      val distWebDir = distDir / "web"
      IO.copyDirectory(appPack, distDir)
      distBinDir.listFiles.foreach(_.setExecutable(true, false))
      distDir
    })
    .aggregate(core, app)
}
