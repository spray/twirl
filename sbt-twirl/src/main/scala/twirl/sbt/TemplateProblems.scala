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
 * This is almost verbatim copied from Play20 sbt-plugin
 * https://github.com/playframework/Play20/raw/217271a2d6834b2abefa8eff070ec680c7956a99/framework/src/sbt-plugin/src/main/scala/PlayReloader.scala
 *
 * JR: I refactored this stuff heavily wrt position mapping and for use of SbtUtils
 */

package twirl.sbt

import java.io.File

import sbt._
import Keys._

object TemplateProblems {
  def remapProblemForGeneratedSources(problem: xsbti.Problem) =
    problem.position.sourceFile.collect {
      // Templates
      case twirl.compiler.MaybeGeneratedSource(generatedSource) => {
        def remapPosition(file: File, line: Int, column: Int, offset: Int): (File, Int) =
          (generatedSource.source.get,
           generatedSource.mapPosition(offset))

        Utilities.problem(
          problem.message,
          problem.severity,
          Utilities.mapPosition(problem.position)(remapPosition)
        )
      }
    }.getOrElse(problem)

  def getProblems(incomplete: Incomplete, streamsManager: Streams): Seq[xsbti.Problem] =
    (Compiler.allProblems(incomplete) ++ extractJavaCErrors(incomplete, streamsManager))
      .map(remapProblemForGeneratedSources)

  def extractJavaCErrors(incomplete: Incomplete, streamsManager: Streams): Seq[xsbti.Problem] =
    Incomplete.linearize(incomplete).filter(i => i.node.isDefined && i.node.get.isInstanceOf[ScopedKey[_]]).flatMap { i =>
      val JavacError = """\[error\]\s*(.*[.]java):(\d+):\s*(.*)""".r
      val JavacErrorInfo = """\[error\]\s*([a-z ]+):(.*)""".r
      val JavacErrorPosition = """\[error\](\s*)\^\s*""".r

      Some {
        var first: (Option[(String, String, String)], Option[Int]) = (None, None)
        var parsed: (Option[(String, String, String)], Option[Int]) = (None, None)
        Output.lastLines(i.node.get.asInstanceOf[ScopedKey[_]], streamsManager).map(_.replace(scala.Console.RESET, "")).map(_.replace(scala.Console.RED, "")).collect {
          case JavacError(file, line, message) => parsed = Some((file, line, message)) -> None
          case JavacErrorInfo(key, message) => parsed._1.foreach {
            o =>
              parsed = Some((parsed._1.get._1, parsed._1.get._2, parsed._1.get._3 + " [" + key.trim + ": " + message.trim + "]")) -> None
          }
          case JavacErrorPosition(pos) => {
            parsed = parsed._1 -> Some(pos.size)
            if (first == (None, None)) {
              first = parsed
            }
          }
        }
        first
      }.collect {
        case (Some(error), maybePosition) =>
          val position =
            Utilities.position(
              path = Some(error._1),
              line = Some(error._2.toInt),
              column = maybePosition.map(_ - 1))

          Utilities.problem(message  = error._3,
                           severity = xsbti.Severity.Error,
                           position = position)
      }
    }
}
