/* Copyright 2012 Johannes Rudolph
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package twirl.sbt

import sbt._
import Keys._

import java.io.File
import java.nio.charset.Charset

object TwirlPlugin extends Plugin {

  object Twirl extends TwirlKeys {

    def settings = seq(
      twirlTemplatesTypes := Map(
        "html" -> TemplateType("twirl.api.Html", "twirl.api.HtmlFormat"),
        "txt"  -> TemplateType("twirl.api.Txt", "twirl.api.TxtFormat"),
        "xml"  -> TemplateType("twirl.api.Xml", "twirl.api.XmlFormat")
      ),

      twirlImports in Global := Nil,

      sourceDirectory in twirlCompile <<= (sourceDirectory in Compile) / "twirl",

      target in twirlCompile <<= (sourceManaged in Compile) / "generated-twirl-sources",

      twirlSourceCharset := Charset.forName("UTF8"),

      twirlCompile <<= (
        sourceDirectory in twirlCompile,
        target in twirlCompile,
        twirlTemplatesTypes,
        twirlSourceCharset,
        twirlImports,
        streams
      ) map TemplateCompiler.compile,

      (sourceGenerators in Compile) <+= twirlCompile,

      (managedSourceDirectories in Compile) <+= target in twirlCompile,

      (compile in Compile) <<= (compile in Compile).dependsOn(twirlCompile),

      twirlReportErrors <<=
        (compile in Compile, streamsManager)
          .mapR(TemplateTasks.improveErrorMsg)
          .triggeredBy(compile in Compile, twirlCompile),

      TemplateTasks.addProblemReporterTo(twirlReportErrors, templatesOrTemplateSources),

      // watch sources support
      includeFilter in twirlCompile := "*.scala.*",
      excludeFilter in twirlCompile <<= excludeFilter in Global,
      watch(sourceDirectory in twirlCompile, includeFilter in twirlCompile, excludeFilter in twirlCompile),

      resolvers += "repo.spray.io" at "http://repo.spray.io",

      libraryDependencies <+= (scalaVersion) { sV =>
        val scalaV = binaryScalaVersion(sV)
        val crossVersionedName = "twirl-api_"+scalaV
        val version = IO.readStream(getClass.getClassLoader.getResourceAsStream("twirl-version")).trim()
        "io.spray" % crossVersionedName % version
      }
    )
  }

  lazy val errorExtensions = Seq(".scala.xml", ".scala.html", ".scala.txt", ".template.scala")
  lazy val templatesOrTemplateSources: File => Boolean =
    file => errorExtensions.exists(file.getName.endsWith)

  def watch(sourceDirKey: SettingKey[File], filterKey: SettingKey[FileFilter], excludeKey: SettingKey[FileFilter]) =
    watchSources <++= (sourceDirKey, filterKey, excludeKey) map descendents
  def descendents(sourceDir: File, filt: FileFilter, excl: FileFilter) =
    sourceDir.descendantsExcept(filt, excl).get

  def binaryScalaVersion(scalaVersion: String): String =
    if (scalaVersion.contains("-")) scalaVersion // pre-release version
    else if (scalaVersion.startsWith("2.9")) "2.9.2"
    else if (scalaVersion.startsWith("2.10")) "2.10"
    else if (scalaVersion.startsWith("2.11")) "2.11"
    else throw new IllegalArgumentException("Unsupported Scala version "+scalaVersion)
}
