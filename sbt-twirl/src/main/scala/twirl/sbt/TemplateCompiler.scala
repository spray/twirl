/* Copyright 2012 Typesafe (http://www.typesafe.com), Johannes Rudolph
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
 *
 * This is an almost verbatim copy of the Play20 sbt-plugin at
 * https://github.com/playframework/Play20/raw/217271a2d6834b2abefa8eff070ec680c7956a99/framework/src/sbt-plugin/src/main/scala/PlayCommands.scala
 */

package twirl.sbt

import sbt._
import xsbti.Severity.Error
import java.io.File
import twirl.compiler._
import collection.Seq

object TemplateCompiler {

  def compile(sourceDirectory: File,
              generatedDir: File,
              templateTypes: PartialFunction[String, TemplateType],
              additionalImports: Seq[String],
              streams: Keys.TaskStreams) = {
    try {
      IO.createDirectory(generatedDir)

      cleanUp(generatedDir)

      val templates = collectTemplates(sourceDirectory, templateTypes)
      streams.log.debug("Preparing " + templates.size + " Twirl template(s) ...")

      for ((templateFile, extension, TemplateType(resultType, formatterType)) <- templates) {
        val addImports = additionalImports.map("import " + _.replace("%format%", extension)).mkString("\n")
        TwirlCompiler.compile(templateFile, sourceDirectory, generatedDir, formatterType, addImports)
      }

      (generatedDir ** "*.template.scala").get.map(_.getAbsoluteFile)

    } catch handleTemplateCompilationError
  }

  private def cleanUp(generatedDir: File) {
    (generatedDir ** "*.template.scala").get.foreach {
      GeneratedSource(_).sync()
    }
  }

  private def collectTemplates(sourceDirectory: File, templateTypes: PartialFunction[String, TemplateType]) = {
    (sourceDirectory ** "*.scala.*").get.flatMap { file =>
      val ext = file.name.split('.').last
      if (templateTypes.isDefinedAt(ext)) Some(file, ext, templateTypes(ext))
      else None
    }
  }

  private val handleTemplateCompilationError: PartialFunction[Throwable, Nothing] = {
    case TemplateCompilationError(source, message, line, column) =>
      throw new TemplateTasks.ProblemException(
        Utilities.problem(message, Error,
          Utilities.position(
            Some(source.getCanonicalPath),
            Some(line),
            Some(column)
          )
        )
      )
    case e => throw e
  }
}
