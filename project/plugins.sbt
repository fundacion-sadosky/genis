// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

//resolvers += Resolver.url("hmrc-sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
//addSbtPlugin("uk.gov.hmrc" % "sbt-git-stamp" % "5.1.0")

addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")

// Use the Play sbt plugin for Play projects
//addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.10")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.19")


//addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.1")
// Not supported in sbt 1.x

// addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")
// Not supported in sbt 1.x

//addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.10")


//addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.0.0")
// Not supported in sbt 1.x


//addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")

// This plugin automatically refreshes Chrome when you make changes to your app
//addSbtPlugin("com.jamesward" %% "play-auto-refresh" % "0.0.11")

//addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")


//addSbtPlugin("com.orrsella" % "sbt-stats" % "1.0.5")
addSbtPlugin("com.orrsella" % "sbt-stats" % "1.0.7")


resolvers += Classpaths.sbtPluginReleases

//addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.99.7.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.8")

//addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.6.0")
addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "1.0.0")