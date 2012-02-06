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

object TemplateTasks {
  class ProblemException(problems: xsbti.Problem*) extends xsbti.CompileFailed {
    def arguments(): Array[String] = Array.empty
    def problems(): Array[xsbti.Problem] = problems.toArray
  }

  def improveErrorMsg[T](result: Result[T], streamManagerR: Result[Streams]): T = result match {
    case Inc(incomplete) =>
      val probs = TemplateProblems.getProblems(incomplete, streamManagerR.toEither.right.get)

      throw new ProblemException(probs: _*)
    case Value(v) => v
  }

  lazy val reportErrors = TaskKey[Unit]("report-errors")
  def addProblemReporterTo[T: Manifest](key: TaskKey[T], filter: File => Boolean = _ => true): Setting[_] =
    reportErrors.asInstanceOf[TaskKey[T]] in key <<=
      (key, streams).mapR(reportProblems(filter))
                    .triggeredBy(key)

  def reportProblems[T](filter: File => Boolean)(result: Result[T], streams: Result[TaskStreams]): T = result match {
    case Inc(incomplete) =>
      val reporter = new LoggerReporter(10, streams.toEither.right.get.log)
      Compiler.allProblems(incomplete)
        .filter(_.position.sourceFile.exists(filter))
        .foreach { p =>
          reporter.display(p.position, p.message, p.severity)
        }

      throw incomplete
    case Value(v) => v
  }
}