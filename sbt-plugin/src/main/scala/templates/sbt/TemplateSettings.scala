/* Copyright 2012 Typesafe (http://www.typesafe.com)
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

object TemplateSettings {

  import TemplateKeys._

  def settings = seq(
    templatesTypes := {
      case "html" => ("play.api.templates.Html", "play.api.templates.HtmlFormat")
      case "txt" => ("play.api.templates.Txt", "play.api.templates.TxtFormat")
      case "xml" => ("play.api.templates.Xml", "play.api.templates.XmlFormat")
    },
    templatesImport in Global := Nil,
    templatesSources <<= (sourceDirectory in Compile) / "templates",
    templatesTarget <<= (sourceManaged in Compile) / "generated-template-sources",
    compileTemplates <<= (templatesSources, templatesTarget, templatesTypes, templatesImport) map TemplateCompiler.compile,

    (sourceGenerators in Compile) <+= compileTemplates,
    (managedSourceDirectories in Compile) <+= templatesTarget,
    (compile in Compile) <<= (compile in Compile).dependsOn(compileTemplates),
    templatesReportErrors <<=
      (compile in Compile, streamsManager, streams).mapR(TemplateTasks.improveErrorMsg)
        .triggeredBy(compile in Compile),

    // watch sources support
    includeFilter in compileTemplates := "*.scala.*",
    excludeFilter in compileTemplates <<= excludeFilter in Global,
    watchSources in compileTemplates <<= (templatesSources, includeFilter in compileTemplates, excludeFilter in compileTemplates) map { (sourceDir, filt, excl) =>
      sourceDir.descendentsExcept(filt, excl).get
    },

    watchSources <++= watchSources in compileTemplates
  )
}
