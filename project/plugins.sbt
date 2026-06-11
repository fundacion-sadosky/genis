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
