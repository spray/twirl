import sbt._
import Keys._

object Build extends Build {
  import Dependencies._

  // configure prompt to show current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
  }

  lazy val root =
    Project("twirl", file("."))
      .settings(general: _*)
      .settings(noPublishing: _*)
      .aggregate(twirlApi, twirl, sbtPlugin)

  lazy val twirlApi =
    Project("twirl-api", file("twirl-api"))
      .settings(general: _*)
      .settings(publishing: _*)
      .settings(
        libraryDependencies += commonsLang
      )

  lazy val twirl =
    Project("twirl-compiler", file("twirl-compiler"))
      .settings(general: _*)
      .settings(publishing: _*)
      .settings(
        libraryDependencies ++= Seq(
          scalaIO,
          Test.specs
        ),
        libraryDependencies <+= scalaVersion(scalaCompiler)
      )
      .dependsOn(twirlApi % "test")

  lazy val sbtPlugin =
    Project("sbt-twirl", file("sbt-twirl"))
      .settings(general: _*)
      .settings(publishing: _*)
      .settings(
        Keys.sbtPlugin := true
      )
      .dependsOn(twirl)


  lazy val general = seq(
    version               := "0.5.0-SNAPSHOT",
    homepage              := Some(new URL("https://github.com/spray/sbt-twirl")),
    organization          := "cc.spray",
    organizationHomepage  := Some(new URL("http://spray.cc")),
    startYear             := Some(2012),
    licenses              := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scalaVersion          := "2.9.1",
    scalacOptions         := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
    description           := "The Play framework Scala template engine, standalone and packaged as an SBT plugin"
  )

  lazy val publishing = seq(
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    publishMavenStyle := true,
    publishTo <<= version { version =>
      Some {
        "spray nexus" at {
          // public uri is repo.spray.cc, we use an SSH tunnel to the nexus here
          "http://localhost:42424/content/repositories/" + {
            if (version.trim.endsWith("SNAPSHOT")) "snapshots/" else"releases/"
          }
        }
      }
    }
  )

  lazy val noPublishing = seq(
    publish := (),
    publishLocal := ()
  )
}


