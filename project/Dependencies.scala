import sbt._

object Dependencies {
  val commonsLang = "commons-lang"                  %  "commons-lang"   % "2.6"
  val scalaIO     = "com.github.scala-incubator.io" %% "scala-io-file"  % "0.4.2"

  object Test {
    val specs = "org.specs2" %% "specs2" % "1.12.3" % "test"
  }

  def scalaIO(scalaVersion: String) = scalaVersion match {
    case x if x startsWith "2.9" => "com.github.scala-incubator.io" % "scala-io-file_2.9.2"  % "0.4.1-seq"
    case _ => "com.github.scala-incubator.io" %% "scala-io-file"  % "0.4.2"
  }

  def scalaCompiler(version: String) = "org.scala-lang" % "scala-compiler" % version
}
