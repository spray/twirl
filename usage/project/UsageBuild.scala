import sbt._
import Keys._

object UsageBuild extends Build {
  lazy val usage =
    Project("usage", file("."))
      .settings(templates.sbt.TemplatePlugin.Template.settings: _*)
      .settings(
        libraryDependencies += "commons-lang" % "commons-lang" % "2.6",
        scalacOptions += "-unchecked"
    )
}
