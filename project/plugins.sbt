// Comment to get more information during initialization
logLevel := Level.Warn

// Fix version conflicts
ThisBuild / evictionErrorLevel := Level.Warn
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// ============================================================================
// PLAY FRAMEWORK 3.0
// ============================================================================
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.6")

// ============================================================================
// PACKAGING
// ============================================================================
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.0")

// ============================================================================
// CODE QUALITY
// ============================================================================

// Dependency graph  
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")

// Stats
addSbtPlugin("com.orrsella" % "sbt-stats" % "1.0.7")

// Coverage
resolvers += Classpaths.sbtPluginReleases
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.2.2")

// Scalastyle
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// ============================================================================
// WEB ASSETS - DEPRECATED in Play 3 but keeping for legacy module
// ============================================================================
// Estos plugins solo se usan en el módulo legacy
// En Play 3 se recomienda usar Webpack/Vite, pero mantenemos para legacy

// Comentados por ahora - activar solo para legacy si es necesario
// addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.1")
// addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")
// addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.0")
// addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")
// addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")
