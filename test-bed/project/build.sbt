libraryDependencies ++= Seq(
    "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.0",
    "commons-lang"                  %  "commons-lang"  % "2.6"
)

scalaVersion := "2.9.2"

unmanagedSourceDirectories in Compile <<= Seq(
  baseDirectory / "sbt-twirl-src",
  baseDirectory / "twirl-api-src",
  baseDirectory / "twirl-compiler-src"
).join

unmanagedResourceDirectories in Compile <<= Seq(
  baseDirectory / "sbt-twirl-res"
).join