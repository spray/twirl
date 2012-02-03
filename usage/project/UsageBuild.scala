import sbt._
import Keys._

object UsageBuild extends Build {
  lazy val usage =
    Project("usage", file("."))
      .settings(TemplateSettings.settings: _*)
}
