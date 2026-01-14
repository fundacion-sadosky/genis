// Comment to get more information during initialization
logLevel := Level.Warn

// Resolvers for SBT plugins
resolvers ++= Seq(
  "Maven Central" at "https://repo1.maven.org/maven2/",
  Resolver.sonatypeOssRepos("snapshots").head,
  "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"
)

// Play Framework 3.x Plugin - ONLY THIS
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.6")

// Check for updates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.4")

// =================== Plugins legacy - COMENTADOS PARA PLAY 3 ===================
// Estos plugins no son necesarios ni compatibles con Play 3.x + Scala 3.3.1
// addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")
// addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")
// addSbtPlugin("com.jamesward" %% "play-auto-refresh" % "0.0.11")
// addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")
// addSbtPlugin("com.orrsella" % "sbt-stats" % "1.0.5")
// addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.99.7.1")
// addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.6.0")
// ==================================================================================


