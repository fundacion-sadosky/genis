import sbt._
import Keys._
import Tests._

import play.Play.autoImport._
import PlayKeys._
import play.PlayScala

import com.typesafe.config._

/**
 * This is a simple sbt setup generating Slick code from the given
 * database before compiling the projects code.
 */
object myBuild extends Build {
  lazy val mainProject = Project(
    id = "root",
    base = file("."),
    settings = super.settings ++ Seq(
      slick <<= slickCodeGenTask // register manual sbt command
      )).dependsOn(codegenProject).enablePlugins(PlayScala)

  // code generation task
  lazy val codegenProject = Project(
    id="codegen",
    base=file("codegen"),
    settings = Seq(
      scalaVersion := "2.11.12",
      libraryDependencies ++= List(
        "org.scala-lang" % "scala-reflect" % "2.13.12",
        "com.typesafe.slick" %% "slick" % "2.1.0",
        "com.typesafe.slick" %% "slick-codegen" % "2.1.0"
      ),
      javaOptions ++= Seq("-Xmx2048M", "-Xms512M", "-XX:MaxPermSize=2048M")
    )
  )
  
  lazy val slick = TaskKey[Seq[File]]("slick-codegen")
  lazy val slickCodeGenTask = (scalaSource in Compile, resourceDirectory in Compile, dependencyClasspath in Compile, runner in Compile, streams) map { (srcDir, resDir, cp, r, s) =>
    val outputDir = (srcDir).getPath
    val slickDriver = "scala.slick.driver.PostgresDriver"
    val pkg = "models"
    val url = "jdbc:h2:mem:;INIT=RUNSCRIPT FROM '" + (resDir.getPath) + "/schema.sql';MODE=PostgreSQL"
    val jdbcDriver = "org.h2.Driver"

    toError(r.run("util.PdgSlickCodeGenerator", cp.files, Array(slickDriver, jdbcDriver, url, outputDir, pkg), s.log))
    val fname = outputDir + "/" + pkg + "/Tables.scala"
    Seq(file(fname))
  }
}