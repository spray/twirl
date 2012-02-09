libraryDependencies ++= Seq(
    "com.github.scala-incubator.io" %% "scala-io-file" % "0.2.0",
    "commons-lang"                  %  "commons-lang"  % "2.6"
)

unmanagedSourceDirectories in Compile <<= Seq(
  baseDirectory / "sbt-twirl-src",
  baseDirectory / "twirl-api-src",
  baseDirectory / "twirl-compiler-src"
).join