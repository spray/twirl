package templates.sbt

import sbt._
import Keys._

object TemplateTasks {
  case class ProblemException(problems: Array[xsbti.Problem]) extends xsbti.CompileFailed {
    def arguments(): Array[String] = Array.empty
  }

  def improveErrorMsg(compileR: Result[inc.Analysis], streamManagerR: Result[Streams], streams: Result[TaskStreams]): inc.Analysis = compileR match {
    case Inc(incomplete) =>
      val reporter = new LoggerReporter(10, streams.toEither.right.get.log)
      val probs = TemplateProblems.getProblems(incomplete, streamManagerR.toEither.right.get)
      probs.foreach { p =>
        reporter.display(p.position, p.message, p.severity)
      }

      throw ProblemException(probs.toArray)
    case Value(v) => v
  }
}