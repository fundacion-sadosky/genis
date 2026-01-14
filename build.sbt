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
  // Play Framework 3.x core
  "org.playframework" %% "play" % "3.0.6",
  "org.playframework" %% "play-json" % "3.0.4",
  "org.playframework" %% "play-guice" % "3.0.6",
  
  // ORM - Slick
  "org.playframework" %% "play-slick" % "6.2.0",
  "org.playframework" %% "play-slick-evolutions" % "6.2.0",

  // Database - PostgreSQL direct JDBC
  "org.postgresql" % "postgresql" % "42.7.5",
  
  // LDAP
  "com.unboundid" % "unboundid-ldapsdk" % "7.0.4",
  
  // Seguridad
  "org.mindrot" % "jbcrypt" % "0.4",
  "com.auth0" % "java-jwt" % "4.5.0",
  
  // Utilidades
  "com.google.guava" % "guava" % "33.4.0-jre",
  "org.apache.commons" % "commons-lang3" % "3.17.0",
  "commons-io" % "commons-io" % "2.18.0",
  
  // Logging
  "ch.qos.logback" % "logback-classic" % "1.5.15",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  
  // Testing
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

// ============================================================================
// CONFIGURACIÓN DE COMPILACIÓN
// ============================================================================

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-feature",
  "-deprecation"
)

javacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-source", "17",
  "-target", "17"
)

// ============================================================================
// CONFIGURACIÓN DE DESARROLLO
// ============================================================================

ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

// Force Jackson version alignment (Play 3.0.6 uses Jackson 2.15.x)
dependencyOverrides ++= Seq(
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.4",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.15.4",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.15.4",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.15.4"
)

// ============================================================================
// CONFIGURACIÓN DE PACKAGING
// ============================================================================

packageDescription := "GENis - Sistema de Análisis de Perfiles Genéticos"
maintainer := "Fundación Dr. Manuel Sadosky"

// ============================================================================
// CONFIGURACIÓN DE PLAY
// ============================================================================

// Play 3.x maneja puertos a través de configuración en application.conf

