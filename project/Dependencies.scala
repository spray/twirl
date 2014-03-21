import sbt._

object Dependencies {
  val commonsLang = "commons-lang"                  %  "commons-lang"   % "2.6"
  val scalaIO     = "com.github.scala-incubator.io" %% "scala-io-file"  % "0.4.2"

  def scalaIO(scalaVersion: String) = scalaVersion match {
    case x if x startsWith "2.9" => "com.github.scala-incubator.io" % "scala-io-file_2.9.2"  % "0.4.1-seq"
    case _ => "com.github.scala-incubator.io" %% "scala-io-file"  % "0.4.2"
  }
  def specs2(scalaVersion: String) = scalaVersion match {
    case x if x startsWith "2.11" => "org.specs2" %% "specs2-core" % "2.3.10" % "test"
    case _ => "org.specs2" %% "specs2" % "1.12.3" % "test"
  }
  def scalaXml(scalaVersion: String) =
    CrossVersion.partialVersion(scalaVersion) match {
      // if scala 2.11+ is used, add dependency on scala-xml module
      case Some((2, scalaMajor)) if scalaMajor >= 11 => Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.1")
      case _ => Nil
    }

  def scalaCompiler(version: String) = "org.scala-lang" % "scala-compiler" % version
}
