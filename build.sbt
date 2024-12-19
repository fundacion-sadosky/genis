import WebKeys._
//import com.nescale.gitstamp.GitStampPlugin._
import RjsKeys._

//Seq(gitStampSettings: _*)

name := """genis"""

// Scala Version, Play supports both 2.10 and 2.11
scalaVersion := "2.11.11"

lazy val root = (project in file(".")).enablePlugins(PlayScala)


transitiveClassifiers := Seq("sources", "javadoc")
// TODO Set your organization here; ThisBuild means it will apply to all sub-modules
organization in ThisBuild := "ar.org.fundacionsadosky"

version := "5.1.12.i141.debugging"

packageDescription := "Genis app"

maintainer := "Fundacion Sadosky"

/*
debianPackageDependencies in Debian := Seq(
  "mongodb (>= 3.4.0)", 
  "postgresql (>= 9.0.0)",
  "nginx (>= 1.6.0)",
  "slapd (>= 2.4.40)",
  "ldap-utils",
  "ldapscripts"
)
*/
debianPackageDependencies in Debian := Seq(
  "postgresql",
  "nginx",
  "slapd"
)

linuxPackageMappings in Debian := {
    // mappings: Seq[LinuxPackageMapping]
    val mappings = linuxPackageMappings.value

    // this process will must return another Seq[LinuxPackageMapping]
    mappings map {  linuxPackage =>

        // each mapping element is a Seq[(java.io.File, String)]
        val filtered = linuxPackage.mappings map {
            case (file, name) => file -> name // alter stuff here
        } filterNot {
            case (file, name) => name.contains("/share/doc/")
        }

        // returns a fresh LinuxPackageMapping based on the above
        linuxPackage.copy(
            mappings = filtered
        )
    } filter {
        linuxPackage => linuxPackage.mappings.nonEmpty // return all mappings that are nonEmpty (this effectively removes all empty linuxPackageMappings)
    }
}
  
linuxPackageMappings in Debian ++= {
  import com.typesafe.sbt.packager.MappingsHelper._  

  directory("scripts/provided") filterNot { case (f, _) =>
    f.isDirectory
  } map { case (f, _) =>
    packageMapping( f -> s"/usr/share/${normalizedName.value}/scripts/${f.name}" ) withUser normalizedName.value withGroup normalizedName.value withPerms "0754" 
  }

}

linuxPackageMappings in Debian ++= {
  import com.typesafe.sbt.packager.MappingsHelper._  

  directory("scripts/evolutions") filterNot { case (f, _) =>
    f.isDirectory
  } map { case (f, _) =>
    packageMapping( f -> s"/usr/share/${normalizedName.value}/evolutions/${f.name}" ) withUser normalizedName.value withGroup normalizedName.value withPerms "0754" 
  }

}

linuxPackageMappings in Debian += packageTemplateMapping(s"/var/run/${normalizedName.value}")() withUser normalizedName.value withGroup normalizedName.value

linuxPackageMappings in Debian += packageMapping( file("scripts/init-script.sh") -> s"/etc/init.d/${normalizedName.value}" ) withPerms "0755" 

maintainerScripts in Debian := {
  import DebianConstants._
  import com.typesafe.sbt.packager.MappingsHelper._  
 
  var tmp = (maintainerScripts in Debian).value
  
  directory("scripts/maintainer/postinst").sorted filterNot { case (f, _) =>
    f.isDirectory
  } map { case (f, _) =>
    Postinst -> f 
  } foreach { tup =>
    tmp = maintainerScriptsAppendFromFile(tmp)(tup)   
  }

  tmp
}

// Dependencies
libraryDependencies ++= Seq(
  filters,
  cache,
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
)

// RDBMS
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "0.8.0",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.typesafe.slick" %% "slick-codegen" % "2.1.0",
  "com.github.tminglei" %% "slick-pg" % "0.8.5",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
)

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

// NoSQL
libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.12.0-play23"
)

// Dependency Injection
libraryDependencies ++= Seq(
    "com.google.inject" % "guice" % "3.0",
    "com.google.inject.extensions" % "guice-multibindings" % "3.0",
    "javax.inject" % "javax.inject" % "1"
)

// web assets (i.e. client-side) 
libraryDependencies ++= Seq(
  "org.webjars.npm" % "lodash" % "4.17.4",
  "org.webjars" % "requirejs" % "2.1.14-1",
  "org.webjars" % "underscorejs" % "1.6.0-3",
  "org.webjars" % "jquery" % "3.1.1-1",
  "org.webjars" % "jquery-ui" % "1.11.1",
  "org.webjars" % "bootstrap" % "3.3.7-1" exclude("org.webjars", "jquery"),
  "org.webjars" % "angularjs" % "1.4.0" exclude("org.webjars", "jquery"),
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
  "org.webjars" % "animate.css" % "3.5.2"
  )

// LDAP 
libraryDependencies ++= Seq(
  "com.unboundid" % "unboundid-ldapsdk" % "2.3.1"
)

// CSV	 
libraryDependencies ++= Seq(
  "com.github.tototoshi" %% "scala-csv" % "1.2.0"
)

// Async
libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-async" % "0.9.6"
)

// TOTP 
libraryDependencies ++= Seq(
  "org.jboss.aerogear" % "aerogear-otp-java" % "1.0.0"
)

resolvers += "Cascading Conjars" at "https://conjars.wensel.net/repo/"

// Spark 2.0
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.1.1" withSources() withJavadoc(),
  "org.apache.spark" %% "spark-sql" % "2.1.1",
  //"org.mongodb.spark" % "mongo-spark-connector_2.11" % "2.0.0" withSources() withJavadoc(),
  "org.mongodb" % "mongo-java-driver" % "3.2.2" withSources() withJavadoc(),
  "org.scala-graph" %% "graph-core" % "1.11.5"
)

libraryDependencies ++= Seq(
  ws
)

libraryDependencies += "de.leanovate.play-mockws" %% "play-mockws" % "2.3.2" % Test

// Test
libraryDependencies ++= Seq(
  "org.scalatestplus" % "play_2.11" % "1.2.0" % "test"
)

//Reporting

libraryDependencies ++= Seq(
  // HTML parsing + PDF generation
  //   - http://jtidy.sourceforge.net/
  //   - https://github.com/flyingsaucerproject/flyingsaucer
  //   - https://about.validator.nu/htmlparser/
  "net.sf.jtidy" % "jtidy" % "r938",
  "org.xhtmlrenderer" % "flying-saucer-pdf-itext5" % "9.1.6",
  "nu.validator.htmlparser" % "htmlparser" % "1.4"
)

// Scala Compiler Options
scalacOptions in ThisBuild ++= Seq(
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Xlint", // recommended additional warnings
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code"
)

// Java compiler options
javacOptions in ThisBuild ++= Seq(
  "-source", "1.8",
  "-target", "1.8"
)

//
// sbt-web configuration
// https://github.com/sbt/sbt-web
//

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

JsEngineKeys.command := Some(new sbt.File("//usr//bin//nodejs"))

// Configure the steps of the asset pipeline (used in stage and dist tasks)
// rjs = RequireJS, uglifies, shrinks to one file, replaces WebJars with CDN
// digest = Adds hash to filename
// gzip = Zips all assets, Asset controller serves them automatically when client accepts them
pipelineStages := Seq(digest, rjs, gzip)

// RequireJS with sbt-rjs (https://github.com/sbt/sbt-rjs#sbt-rjs)
// ~~~
RjsKeys.paths += ("jsRoutes" -> ("/jsroutes" -> "empty:"))

RjsKeys.paths += ("appConf" -> ("/appConf" -> "empty:"))

RjsKeys.paths += ("sensitiveOper" -> ("/sensitiveOper" -> "empty:"))

RjsKeys.paths += ("team" -> ("/team" -> "empty:"))

webJarCdns := Map("org.webjars" -> "//cdn.jsdelivr.net/webjars")

includeFilter in (Assets, LessKeys.less) := "mainTest.less" | "main.less" | "mainAmarillo.less" | "mainAzul.less" | "mainVerde.less"

//RjsKeys.mainModule := "main"

// Asset hashing with sbt-digest (https://github.com/sbt/sbt-digest)
// ~~~
// md5 | sha1
//DigestKeys.algorithms := "md5"
//includeFilter in digest := "..."
//excludeFilter in digest := "..."

// HTTP compression with sbt-gzip (https://github.com/sbt/sbt-gzip)
// ~~~
// includeFilter in GzipKeys.compress := "*.html" || "*.css" || "*.js"
// excludeFilter in GzipKeys.compress := "..."

// JavaScript linting with sbt-jshint (https://github.com/sbt/sbt-jshint)
// ~~~
// JshintKeys.config := ".jshintrc"

// All work and no play...
emojiLogs

net.virtualvoid.sbt.graph.Plugin.graphSettings

instrumentSettings

ScoverageKeys.excludedPackages in ScoverageCompile := "<empty>;models\\..*;views\\..*;"

ScoverageKeys.minimumCoverage := 80

ScoverageKeys.failOnMinimumCoverage := false

ScoverageKeys.highlighting := false

