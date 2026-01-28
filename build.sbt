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
// PROYECTO LEGACY - Scala 2.11 + Play 2.3 (código existente)
// ============================================================================
// TEMPORALMENTE COMENTADO: Play 2.3 no es compatible con el plugin de Play 3.0
// que instalamos en plugins.sbt. Vamos a migrar módulo por módulo a core/.
// El código legacy sigue en modules/legacy/ pero no se compila por ahora.
/*
lazy val legacy = (project in file("modules/legacy"))
  .enablePlugins(PlayScala)
  .dependsOn(shared)
  .settings(
    name := "genis-legacy",
    scalaVersion := "2.11.12",
    
    // Java 8 settings
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions ++= Seq(
      "-target:jvm-1.8",
      "-encoding", "UTF-8",
      "-deprecation",
      "-feature",
      "-unchecked"
    ),
    
    // Play 2.3 dependencies (original)
    libraryDependencies ++= Seq(
      // cache and filters are now explicit in Play 3
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
    ),
    
    // RDBMS  
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-slick" % "0.8.0",
      "com.typesafe.slick" %% "slick" % "2.1.0",
      "com.typesafe.slick" %% "slick-codegen" % "2.1.0",
      "com.github.tminglei" %% "slick-pg" % "0.8.5",
      "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
    ),
    
    // NoSQL
    libraryDependencies ++= Seq(
      "org.reactivemongo" %% "play2-reactivemongo" % "0.12.0-play23"
    ),
    
    // Dependency Injection
    libraryDependencies ++= Seq(
      "com.google.inject" % "guice" % "3.0",
      "com.google.inject.extensions" % "guice-multibindings" % "3.0",
      "javax.inject" % "javax.inject" % "1"
    ),
    
    // LDAP
    libraryDependencies ++= Seq(
      "com.unboundid" % "unboundid-ldapsdk" % "2.3.1"
    ),
    
    // CSV
    libraryDependencies ++= Seq(
      "com.github.tototoshi" %% "scala-csv" % "1.2.0"
    ),
    
    // Async
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-async" % "0.9.6"
    ),
    
    // TOTP
    libraryDependencies ++= Seq(
      "org.jboss.aerogear" % "aerogear-otp-java" % "1.0.0"
    ),
    
    // Spark 2.0
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "2.1.1" withSources() withJavadoc(),
      "org.apache.spark" %% "spark-sql" % "2.1.1",
      "org.mongodb" % "mongo-java-driver" % "3.2.2" withSources() withJavadoc(),
      "org.scala-graph" %% "graph-core" % "1.11.5"
    ),
    
    libraryDependencies ++= Seq(
      ws
    ),
    
    // Test
    libraryDependencies ++= Seq(
      "org.scalatestplus" % "play_2.11" % "1.2.0" % "test",
      "de.leanovate.play-mockws" %% "play-mockws" % "2.3.2" % Test
    ),
    
    // Reporting
    libraryDependencies ++= Seq(
      "net.sf.jtidy" % "jtidy" % "r938",
      "org.xhtmlrenderer" % "flying-saucer-pdf-itext5" % "9.1.6",
      "nu.validator.htmlparser" % "htmlparser" % "1.4"
    ),
    
    // Web assets
    libraryDependencies ++= Seq(
      "org.webjars.npm" % "lodash" % "4.17.4",
      "org.webjars" % "requirejs" % "2.1.14-1",
      "org.webjars" % "underscorejs" % "1.6.0-3",
      "org.webjars" % "jquery" % "3.1.1-1",
      "org.webjars" % "jquery-ui" % "1.11.1",
      "org.webjars" % "bootstrap" % "3.3.7-1" exclude("org.webjars", "jquery"),
      "org.webjars" % "angular js" % "1.4.0" exclude("org.webjars", "jquery"),
      "org.webjars" % "angular-ui-bootstrap" % "0.13.3",
      "org.webjars" % "angular-ui-select" % "0.13.1",
      "org.webjars" % "angular-ui-sortable" % "0.13.0",
      "org.webjars" % "cryptojs" % "3.1.2",
      "org.webjars" % "angular-file-upload" % "4.1.3",
      "org.webjars" % "angular-hotkeys" % "1.4.0",
      "org.webjars" % "i18next" % "1.7.3",
      "org.webjars" % "ng-i18next" % "0.3.2",
      "org.webjars" % "qrcodejs" % "07f829d",
      "org.webjars" % "font-awesome" % "4.4.0",
      "org.webjars" % "d3js" % "3.5.5-1",
      "org.webjars" % "dagre-d3" % "0.4.10",
      "org.webjars" % "animate.css" % "3.5.2",
      "org.webjars.npm" % "jszip" % "3.10.1"
    ),
    
    resolvers ++= Seq(
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "Cascading Conjars" at "https://conjars.wensel.net/repo/"
    )
  )
*/

// ============================================================================
// PROYECTO CORE - Scala 3 + Play 3 (código nuevo)
// ============================================================================
lazy val core = (project in file("modules/core"))
  .enablePlugins(PlayScala)
  .dependsOn(shared)  // legacy comentado temporalmente
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
    
    // Fix Jackson version conflict
    dependencyOverrides ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.14.3",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.14.3",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.14.3",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.3"
    )
  )

// ============================================================================
// ROOT PROJECT - Agregador
// ============================================================================
lazy val root = (project in file("."))
  .aggregate(shared, core)  // legacy comentado temporalmente
  .dependsOn(core)
  .enablePlugins(PlayScala)
  .settings(
    name := "genis",
    scalaVersion := "3.3.1",
    
    // El proyecto root delega a core por defecto
    publish / skip := true
  )

// ============================================================================
// Configuraciones comunes de sbt-web (DEPRECATED - solo era para legacy)
// ============================================================================
// Estas configuraciones son del viejo sistema de assets de Play 2.3
// En Play 3 se recomienda usar Webpack/Vite
// Las comentamos por ahora

/*
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

includeFilter in (Assets, JshintKeys.jshint) := NothingFilter

// Concurrency restrictions
concurrentRestrictions in ThisBuild := Seq(
  Tags.limitAll(1)
)

JsEngineKeys.npmTimeout := 180.seconds
*/
