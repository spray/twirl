import sbt._
import Keys._
import twirl.sbt.TwirlPlugin._

object Build extends Build {

  lazy val testBed =
    Project("test-bed", file("."))
      .settings(Twirl.settings: _*)
      .settings(
        libraryDependencies += "commons-lang" % "commons-lang" % "2.6",
        scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")
      )

}
