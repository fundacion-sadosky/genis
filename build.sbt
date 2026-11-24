// Build of the public GENis repository: compiles and runs the LR calculation engine
// without the rest of the system (no web layer, databases, LDAP or Spark).
//
// Requires Java 8.   sbt compile   |   sbt test

name := "genis-lr-engine"

// Nothing here is published as a library.
publishArtifact := false
publishMavenStyle := false

scalaVersion := "2.11.11"

// The validation harness (validation/scala) is compiled in its own configuration, so that
// `sbt test` stays the short run of the reference cases.
//   sbt validacion:test   |   validation/correr --lista
lazy val Validacion = config("validacion") extend Test

lazy val root = (project in file("."))
  .configs(Validacion)
  .settings(inConfig(Validacion)(Defaults.testSettings): _*)
  .settings(sourceDirectory in Validacion := baseDirectory.value / "validation")
  .settings(javaOptions in Validacion += "-Xmx6g")

// Sources keep the layout they have in GENis: app/<package>/File.scala
scalaSource in Compile := baseDirectory.value / "app"
scalaSource in Test := baseDirectory.value / "test"
resourceDirectory in Compile := baseDirectory.value / "conf"

// standalone/ holds the only source that is not a whole-file copy from GENis: the verbatim
// extract of `object MutationService` (see standalone/pedigree/MutationServiceObject.scala).
unmanagedSourceDirectories in Compile += baseDirectory.value / "standalone"

// Orchestration files: published for reading, but not compiled, because they depend on
// parts of the system this repository does not include (persistence, services, Spark).
val readOnly = Set(
  "app/configdata/QualityParamsProvider.scala",
  "app/matching/MatchingCalculatorService.scala",
  "app/pedigree/BayesianNetworkService.scala",
  "app/pedigree/MutationService.scala",
  "app/pedigree/PedigreeGenotypificationService.scala",
  "app/pedigree/PedigreeSparkMatcher.scala",
  "app/probability/CalculationTypeService.scala",
  "app/probability/ProbabilityModule.scala",
  "app/probability/ProbabilityService.scala",
  "app/stats/PopulationBaseFrequencyService.scala"
)

excludeFilter in (Compile, unmanagedSources) := (excludeFilter in (Compile, unmanagedSources)).value || {
  val base = baseDirectory.value
  val excluded = readOnly.map(p => (base / p).getCanonicalPath)
  new SimpleFileFilter(f => excluded.contains(f.getCanonicalPath))
}

// Required: netty-http-pipelining 1.1.2 and play-ws 2.3.0 are not on Maven Central.
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Same versions GENis uses. The Mongo/BSON libraries are here because the data types carry
// their serialization formats; no database is contacted.
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.3.10",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.12.0-play23",
  "org.mongodb" % "mongo-java-driver" % "3.2.2",
  "org.scala-graph" %% "graph-core" % "1.11.5",
  // An import in profile/AlleleValue.scala needs it on the classpath; unused when calculating.
  "org.apache.hadoop" % "hadoop-common" % "2.2.0" intransitive(),
  "com.typesafe.play" %% "play-test" % "2.3.10" % Test,
  "org.scalatestplus" %% "play" % "1.2.0" % Test,
  "com.github.tototoshi" %% "scala-csv" % "1.2.0" % Test
)

scalacOptions ++= Seq("-target:jvm-1.8", "-encoding", "UTF-8", "-feature")

// The forensic engine starts a minimal Play application per suite: separate JVM, in sequence.
fork in Test := true
parallelExecution in Test := false
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
