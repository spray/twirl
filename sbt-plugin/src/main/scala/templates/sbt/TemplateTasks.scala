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
  def addProblemReporterTo[T: Manifest](key: TaskKey[T]): Setting[_] =
    reportErrors.asInstanceOf[TaskKey[T]] in key <<=
      (key, streams).mapR(reportProblems)
                    .triggeredBy(key)

  def reportProblems[T](result: Result[T], streams: Result[TaskStreams]): T = result match {
    case Inc(incomplete) =>
      val reporter = new LoggerReporter(10, streams.toEither.right.get.log)
      Compiler.allProblems(incomplete).foreach { p =>
        reporter.display(p.position, p.message, p.severity)
      }

      throw incomplete
    case Value(v) => v
  }
}