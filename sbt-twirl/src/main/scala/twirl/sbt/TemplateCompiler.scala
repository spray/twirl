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

object TemplateCompiler {

  def compile(sourceDirectory: File, generatedDir: File, templateTypes: PartialFunction[String, (String, String)],
              additionalImports: Seq[String]) = {
    IO.createDirectory(generatedDir)

    val templateExt: PartialFunction[File, (File, String, String, String)] = {
      case p if templateTypes.isDefinedAt(p.name.split('.').last) =>
        val extension = p.name.split('.').last
        val exts = templateTypes(extension)
        (p, extension, exts._1, exts._2)
    }

    // deletes old artifacts
    (generatedDir ** "*.template.scala").get.map(GeneratedSource(_)).foreach(_.sync())

    try {
      (sourceDirectory ** "*.scala.*").get.collect(templateExt).foreach {
        case (template, extension, t, format) =>
          TwirlCompiler.compile(
            template,
            sourceDirectory,
            generatedDir,
            t,
            format,
            additionalImports.map("import " + _.replace("%format%", extension)).mkString("\n"))
      }
    } catch {
      case TemplateCompilationError(source, message, line, column) => {
        throw new TemplateTasks.ProblemException(
          SbtUtils.problem(message,
                           Error,
                           SbtUtils.position(
                             Some(source.getCanonicalPath),
                             Some(line),
                             Some(column)
                           ))
        )
      }
      case e => throw e
    }

    (generatedDir ** "*.template.scala").get.map(_.getAbsoluteFile)
  }
}
