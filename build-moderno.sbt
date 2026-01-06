import play.sbt.PlayImport._
import scala.concurrent.duration._

// ============================================================================
// GENIS - Proyecto Moderno con Play Framework 3.x y Scala 3
// ============================================================================

name := """genis"""
organization := "ar.org.fundacionsadosky"
version := "6.0.0.develop"

scalaVersion := "3.3.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

// ============================================================================
// DEPENDENCIAS PRINCIPALES
// ============================================================================

libraryDependencies ++= Seq(
  // Play Framework 3.x
  play,
  "org.playframework" %% "play-json" % "3.0.0",
  "org.playframework" %% "play-slick" % "6.0.0",
  "org.playframework" %% "play-slick-evolutions" % "6.0.0",
  
  // Database - PostgreSQL
  "org.postgresql" % "postgresql" % "42.7.1",
  
  // Slick para acceso a datos
  "com.typesafe.slick" %% "slick" % "3.4.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
  
  // LDAP
  "org.springframework.security" % "spring-ldap-core" % "3.1.1",
  "com.unboundid" % "unboundid-ldapsdk" % "7.0.1",
  
  // Seguridad
  "org.mindrot" % "jbcrypt" % "0.4",
  "com.auth0" % "java-jwt" % "4.4.0",
  
  // Utilidades
  "com.google.guava" % "guava" % "33.0.0-jre",
  "org.apache.commons" % "commons-lang3" % "3.14.0",
  "commons-io" % "commons-io" % "2.15.1",
  
  // Logging
  "ch.qos.logback" % "logback-classic" % "1.5.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  
  // Testing
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

// ============================================================================
// CONFIGURACIÓN DE COMPILACIÓN
// ============================================================================

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-feature",
  "-deprecation",
  "-Xfatal-warnings"
)

javacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-source", "11",
  "-target", "11"
)

// ============================================================================
// CONFIGURACIÓN DE DESARROLLO
// ============================================================================

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

// ============================================================================
// CONFIGURACIÓN DE PACKAGING
// ============================================================================

packageDescription := "GENis - Sistema de Análisis de Perfiles Genéticos"
maintainer := "Fundación Dr. Manuel Sadosky"

// ============================================================================
// CONFIGURACIÓN DE PLAY
// ============================================================================

PlayKeys.playDefaultPort := 9000

// Lazy loading para mejor rendimiento
playDefaultPort := 9000

// ============================================================================
// CONFIGURACIÓN DE EVOLUTIONS
// ============================================================================

slick.codegen.SourceManaged := baseDirectory.value / "app" / "models"
slick.codegen.OutputPackage := "models"
