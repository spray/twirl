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

      libraryDependencies <+= (scalaVersion) { sV =>
        val scalaV = if (CrossVersion.isStable(sV)) CrossVersion.binaryScalaVersion(sV) else sV
        val version = IO.readStream(getClass.getClassLoader.getResourceAsStream("twirl-version"))
        "io.spray" %% "twirl-api" % version from
          "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/io.spray/twirl-api_%s/%s/jars/twirl-api_%s.jar".format(scalaV, version, scalaV)
      }
    )

  }

  lazy val errorExtensions = Seq(".scala.xml", ".scala.html", ".scala.txt", ".template.scala")
  lazy val templatesOrTemplateSources: File => Boolean =
    file => errorExtensions.exists(file.getName.endsWith)

  def watch(sourceDirKey: SettingKey[File], filterKey: SettingKey[FileFilter], excludeKey: SettingKey[FileFilter]) =
    watchSources <++= (sourceDirKey, filterKey, excludeKey) map descendents
  def descendents(sourceDir: File, filt: FileFilter, excl: FileFilter) =
    sourceDir.descendentsExcept(filt, excl).get
}
