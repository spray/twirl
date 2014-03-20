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

  /*
   * Add scala-xml dependency when needed (for Scala 2.11 and newer) in a robust way
   * This mechanism supports cross-version publishing
   */
  private def scalaXmlModule: Setting[Seq[sbt.ModuleID]] = libraryDependencies := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      // if scala 2.11+ is used, add dependency on scala-xml module
      case Some((2, scalaMajor)) if scalaMajor >= 11 =>
        libraryDependencies.value :+ "org.scala-lang.modules" %% "scala-xml" % "1.0.0"
      case _ =>
        libraryDependencies.value
    }
  }

  lazy val twirlApi =
    Project("twirl-api", file("twirl-api"))
      .settings(general: _*)
      .settings(apiPublishing: _*)
      .settings(
        libraryDependencies ++= Seq(
          commonsLang,
          Test.specs
        ),
        scalaXmlModule,
        crossScalaVersions := Seq("2.9.2", "2.10.3")
      )

  lazy val twirlCompiler =
    Project("twirl-compiler", file("twirl-compiler"))
      .settings(general: _*)
      // this doesn't work any more because twirl-compiler doesn't seem to be auto-imported to
      // bintray
      //.settings(publishing: _*)
      .settings(apiPublishing: _*) // use this to publish to repo.spray.io as well
      .settings(
        resolvers += "repo.spray.io" at "http://repo.spray.io",
        libraryDependencies ++= Seq(
          Test.specs
        ),
        libraryDependencies <++= scalaVersion { v =>
          Seq(
            scalaCompiler(v),
            scalaIO(v)
          )
        },
        scalaVersion <<= scalaVersion in LocalProject("sbt-twirl"),
        // add scala-XXX versioned source directories for making backporting easier
        unmanagedSourceDirectories in Compile <+= (sourceDirectory in Compile, scalaBinaryVersion) {
          (base, version) => base / ("scala-"+version)
        }
      )
      .dependsOn(twirlApi % "test")

  lazy val sbtTwirl: Project =
    Project("sbt-twirl", file("sbt-twirl"))
      .settings(general: _*)
      //.settings(publishing: _*)
      .settings(net.virtualvoid.sbt.cross.CrossPlugin.crossBuildingSettings: _*)
      .settings(apiPublishing: _*) // use this to publish to repo.spray.io as well
      .settings(
        Keys.sbtPlugin := true,
        CrossBuilding.crossSbtVersions := Seq("0.12", "0.13")
      )
      .dependsOn(twirlCompiler)
      .aggregate(twirlCompiler)


  lazy val general = seq(
    version               := IO.read(file("sbt-twirl/src/main/resources/twirl-version")).trim,
    homepage              := Some(new URL("https://github.com/spray/sbt-twirl")),
    organization          := "io.spray",
    organizationHomepage  := Some(new URL("http://spray.io")),
    startYear             := Some(2012),
    licenses              := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scalaBinaryVersion   <<= scalaVersion(sV => if (CrossVersion.isStable(sV)) CrossVersion.binaryScalaVersion(sV) else sV),
    scalacOptions         := Seq("-unchecked", "-deprecation", "-encoding", "utf8"),
    description           := "The Play framework Scala template engine, standalone and packaged as an SBT plugin",
    resolvers             += "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
    scalaVersion          := "2.10.3"
  )

  lazy val publishing = seq(
    publishMavenStyle := false,
    publishTo <<= (version) { version: String =>
      val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-"
      val suffix = if (version.contains("-SNAPSHOT")) "snapshots" else "releases"

      val name = "plugin-" + suffix
      val url  = scalasbt + suffix

      Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
    }
  )

  // We publish the api to our own repository
  lazy val apiPublishing = seq(
    publishMavenStyle := true,

    publishTo <<= version { version =>
      Some {
        "spray repo" at {
          // public uri is repo.spray.io, we use an SSH tunnel to the nexus here
          "http://localhost:42424/content/repositories/" + {
            if (version.trim.endsWith("SNAPSHOT")) "snapshots/" else "releases/"
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


