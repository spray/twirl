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
 * I added pointer space reporting and line content.
 */

package templates.sbt

import sbt._
import Keys._

object TemplateProblems {
  def remapProblemForGeneratedSources(problem: xsbti.Problem) = {
    problem.position.sourceFile.collect {

      // Templates
      case play.templates.MaybeGeneratedSource(generatedSource) => {
        new xsbti.Problem {
          def message = problem.message

          def position = new xsbti.Position {
            def line = {
              problem.position.line.map(l => generatedSource.mapLine(l.asInstanceOf[Int])).map(l => xsbti.Maybe.just(l.asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
            }

            def lineContent = IO.readLines(sourceFile.get)(line.get - 1)

            def offset = xsbti.Maybe.nothing[java.lang.Integer]

            def pointer = {
              problem.position.offset.map {
                offset =>
                  generatedSource.mapPosition(offset.asInstanceOf[Int]) - IO.read(generatedSource.source.get).split('\n').take(problem.position.line.map(l => generatedSource.mapLine(l.asInstanceOf[Int])).get - 1).mkString("\n").size - 1
              }.map {
                p =>
                  xsbti.Maybe.just(p.asInstanceOf[java.lang.Integer])
              }.getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
            }

            def pointerSpace = xsbti.Maybe.just(" " * pointer.get)

            def sourceFile = xsbti.Maybe.just(generatedSource.source.get)

            def sourcePath = xsbti.Maybe.just(sourceFile.get.getCanonicalPath)
          }

          def severity = problem.severity
        }
      }

    }.getOrElse {
      problem
    }

  }

  def getProblems(incomplete: Incomplete, streamsManager: Streams): Seq[xsbti.Problem] = {
    (Compiler.allProblems(incomplete) ++ {
      Incomplete.linearize(incomplete).filter(i => i.node.isDefined && i.node.get.isInstanceOf[ScopedKey[_]]).flatMap {
        i =>
          val JavacError = """\[error\]\s*(.*[.]java):(\d+):\s*(.*)""".r
          val JavacErrorInfo = """\[error\]\s*([a-z ]+):(.*)""".r
          val JavacErrorPosition = """\[error\](\s*)\^\s*""".r

          //Project.evaluateTask(streamsManager, state).get.toEither.right.toOption.map { streamsManager =>
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
                if (first ==(None, None)) {
                  first = parsed
                }
              }
            }
            first
          }.collect {
            case (Some(error), maybePosition) => new xsbti.Problem {
              def message = error._3

              def position = new xsbti.Position {
                def line = xsbti.Maybe.just(error._2.toInt)

                def lineContent = IO.readLines(sourceFile.get)(line.get - 1)

                def offset = xsbti.Maybe.nothing[java.lang.Integer]

                def pointer = maybePosition.map(pos => xsbti.Maybe.just((pos - 1).asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])

                def pointerSpace = xsbti.Maybe.just(" " * pointer.get)

                def sourceFile = xsbti.Maybe.just(file(error._1))

                def sourcePath = xsbti.Maybe.just(error._1)
              }

              def severity = xsbti.Severity.Error
            }
          }

      }
    }).map(remapProblemForGeneratedSources)
  }

}
