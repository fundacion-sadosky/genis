import scala.concurrent.duration._

// These old imports are removed (from sbt-web plugins that are deprecated)
// import WebKeys._
// import RjsKeys._

name := "genis"

// Configuración común para todos los proyectos
ThisBuild / organization := "ar.org.fundacionsadosky"
ThisBuild / version := "5.2.0-SNAPSHOT"

// ============================================================================
// PROYECTO SHARED - Cross-compiled entre Scala 2.11 y Scala 3
// ============================================================================
lazy val shared = (project in file("modules/shared"))
  .settings(
    name := "genis-shared",
    // Cross-compilation: compila para ambas versiones
    crossScalaVersions := Seq("2.11.12", "3.3.1"),
    scalaVersion := "3.3.1",
    
    libraryDependencies ++= Seq(
      // Play JSON 3.0 es compatible con Scala 3
      "org.playframework" %% "play-json" % "3.0.6"
    )
  )

// ============================================================================
// PROYECTO CORE - Scala 3 + Play 3 (código nuevo)
// ============================================================================
lazy val core = (project in file("modules/core"))
  .enablePlugins(PlayScala)
  .dependsOn(shared)
  .settings(
    name := "genis-core",
    scalaVersion := "3.3.1",
    
    // Java 17 settings
    javacOptions ++= Seq("-source", "17", "-target", "17"),
    scalacOptions ++= Seq(
      "-release:17",
      "-encoding", "UTF-8",
      "-deprecation",
      "-feature",
      "-unchecked"
    ),
    
    libraryDependencies ++= Seq(
      guice,
      ws,
      
      // Play 3.0
      "org.playframework" %% "play" % "3.0.6",
      "org.playframework" %% "play-json" % "3.0.6",
      
      // Slick (sin play-slick por ahora, no hay versión para Scala 3)
      // Usaremos Slick vanilla directamente
      "com.typesafe.slick" %% "slick" % "3.5.2",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.5.2",
      "org.postgresql" % "postgresql" % "42.7.5",
      
      // Utilidades actualizadas
      "com.google.guava" % "guava" % "33.4.0-jre",
      "org.apache.commons" % "commons-lang3" % "3.17.0",
      "commons-io" % "commons-io" % "2.18.0",
      
      // Logback
      "ch.qos.logback" % "logback-classic" % "1.5.15",
      "ch.qos.logback" % "logback-core" % "1.5.15",
      
      // JWT actualizado
      "com.auth0" % "java-jwt" % "4.5.0",
      
      // LDAP actualizado
      "com.unboundid" % "unboundid-ldapsdk" % "7.0.4",
      
      // Testing
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    ),
    
    // Modern corre en puerto 9001 (legacy usa 9000)
    PlayKeys.playDefaultPort := 9001,
    
    // Fix Jackson version conflict
    dependencyOverrides ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.3",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.14.3",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.14.3",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.3"
    )
  )

// ============================================================================
// ROOT PROJECT - Agregador (solo usa modules/core, NO app/)
// ============================================================================
lazy val root = (project in file("."))
  .aggregate(shared, core)
  .dependsOn(core)
  // NO .enablePlugins(PlayScala) aquí - eso hace que busque /app y /conf
  .settings(
    name := "genis",
    scalaVersion := "3.3.1",
    
    // Delegar 'run' a core/run
    run := (core / Compile / run).evaluated,
    
    // El proyecto root delega a core por defecto
    publish / skip := true
  )
