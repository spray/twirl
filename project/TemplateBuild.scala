import sbt._
import Keys._

object TemplateBuild extends Build {
  import Dependencies._

  lazy val root =
    Project("root", file("."))
      .aggregate(templateApi, templates, sbtPlugin)

  lazy val templateApi =
    Project("template-api", file("template-api"))
      .settings(generalSettings: _*)
      .settings(
        name := "splay-template-api",
        libraryDependencies += commonsLang
      )

  lazy val templates =
    Project("templates", file("templates"))
      .settings(generalSettings: _*)
      .settings(
        name := "splay-template-compiler",
        libraryDependencies ++= Seq(
          scalaIO,
          Test.specs
        ),
        libraryDependencies <+= scalaVersion(scalaCompiler)
      )
      .dependsOn(templateApi % "test")

  lazy val sbtPlugin =
    Project("sbt-plugin", file("sbt-plugin"))
      .settings(generalSettings: _*)
      .settings(
        name := "splay-template-plugin",
        Keys.sbtPlugin := true
      )
      .dependsOn(templates)

  lazy val generalSettings = seq(
    organization := "cc.spray",
    scalacOptions ++= Seq("-unchecked", "-encoding", "utf8", "-deprecation")
  ) ++ publishSettings

  lazy val publishSettings = seq(
    version := "0.5.0-SNAPSHOT",

    // publishing
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    publishMavenStyle := true,
    publishTo <<= version { version =>
      Some {
        "spray repo" at {
          // public uri is repo.spray.cc, we use an SSH tunnel to the nexus here
          "http://localhost:42424/content/repositories/" + {
            if (version.trim.endsWith("SNAPSHOT")) "snapshots/" else"releases/"
          }
        }
      }
    }
  )
}

object Dependencies {
  val commonsLang = "commons-lang"                  %  "commons-lang"   % "2.6"
  val scalaIO     = "com.github.scala-incubator.io" %% "scala-io-file"  % "0.2.0"

  def scalaCompiler(version: String) =
                    "org.scala-lang"                %  "scala-compiler" %  version

  object Test {
    val specs     = "org.specs2"                    %%   "specs2"       %   "1.7.1"    %   "test"
  }
}
