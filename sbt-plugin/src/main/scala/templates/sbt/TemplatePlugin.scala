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

package templates.sbt

import sbt._
import Keys._

import java.io.File
import play.templates._

object TemplatePlugin extends Plugin {

  object Template extends TemplateKeys {
    def settings = seq(
      templatesTypes := {
        case "html" => ("play.api.templates.Html", "play.api.templates.HtmlFormat")
        case "txt" => ("play.api.templates.Txt", "play.api.templates.TxtFormat")
        case "xml" => ("play.api.templates.Xml", "play.api.templates.XmlFormat")
      },
      templatesImport in Global := Nil,
      sourceDirectory in compileTemplates <<= (sourceDirectory in Compile) / "templates",
      target in compileTemplates <<= (sourceManaged in Compile) / "generated-template-sources",
      compileTemplates <<= (sourceDirectory in compileTemplates,
                            target in compileTemplates,
                            templatesTypes,
                            templatesImport) map TemplateCompiler.compile,

      (sourceGenerators in Compile) <+= compileTemplates,
      (managedSourceDirectories in Compile) <+= target in compileTemplates,
      (compile in Compile) <<= (compile in Compile).dependsOn(compileTemplates),
      templatesReportErrors <<=
        (compile in Compile, streamsManager)
          .mapR(TemplateTasks.improveErrorMsg)
          .triggeredBy(compile in Compile, compileTemplates),
      TemplateTasks.addProblemReporterTo(templatesReportErrors, templatesOrTemplateSources),

      // watch sources support
      includeFilter in compileTemplates := "*.scala.*",
      excludeFilter in compileTemplates <<= excludeFilter in Global,
      watch(sourceDirectory in compileTemplates, includeFilter in compileTemplates, excludeFilter in compileTemplates),

      libraryDependencies += "cc.spray" %% "splay-templates-api" % "0.5.0-SNAPSHOT"
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
