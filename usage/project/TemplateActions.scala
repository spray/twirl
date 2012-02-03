import sbt._
import Keys._

import java.io.File
import play.templates._

case class TemplateCompilationException(source: File, message: String, atLine: Int, column: Int) extends Exception(
  "Compilation error: "+message) {
  def line = Some(atLine)
  def position = Some(column)
  def input = Some(scalax.file.Path(source))
  def sourceName = Some(source.getAbsolutePath)
}

object TemplateSettings {
  import TemplateKeys._

  def settings = seq(
    templatesTypes := {
      case "html" => ("play.api.templates.Html", "play.api.templates.HtmlFormat")
      case "txt" => ("play.api.templates.Txt", "play.api.templates.TxtFormat")
      case "xml" => ("play.api.templates.Xml", "play.api.templates.XmlFormat")
    },
    templatesImport := Nil,
    templatesSources <<= (sourceDirectory in Compile) / "templates",
    templatesTarget <<= (sourceManaged in Compile) / "template-sources",
    compileTemplates <<= (templatesSources, templatesTarget, templatesTypes, templatesImport) map TemplateTasks.compileTemplates,

    (sourceGenerators in Compile) <+= compileTemplates,
    (managedSourceDirectories in Compile) <+= templatesTarget,
    (compile in Compile) <<= (compile in Compile).dependsOn(compileTemplates),
    templatesReportErrors <<=
      (compile in Compile, streamsManager, streams).mapR(TemplateTasks.improveErrorMsg).triggeredBy(compile in Compile)
  )
}

object TemplateKeys {
  val templatesSources = SettingKey[File]("templates-sources", "Templates source directory")
  val templatesTarget = SettingKey[File]("templates-target", "Target directory for generated template sources")

  val templatesImport = SettingKey[Seq[String]]("play-templates-imports")
  val templatesTypes = SettingKey[PartialFunction[String, (String, String)]]("play-templates-formats")
  val compileTemplates = TaskKey[Seq[File]]("compile-templates", "Compile scala templates into scala files")
  val templatesReportErrors = TaskKey[inc.Analysis]("templates-report-errorrs", "Reports template errors")
}

object TemplateTasks {
  val compileTemplates = (sourceDirectory: File, generatedDir: File, templateTypes: PartialFunction[String, (String, String)], additionalImports: Seq[String]) => {
      import play.templates._
    println("sources: %s, generated: %s, types: %s, imports: %s" format (sourceDirectory, generatedDir, templateTypes, additionalImports))
      IO.createDirectory(generatedDir)

      val templateExt: PartialFunction[File, (File, String, String, String)] = {
        case p if templateTypes.isDefinedAt(p.name.split('.').last) =>
          val extension = p.name.split('.').last
          val exts = templateTypes(extension)
          val res = (p, extension, exts._1, exts._2)
          println("Found "+res)
          res
      }
      (generatedDir ** "*.template.scala").get.map(GeneratedSource(_)).foreach(_.sync())
      try {

        (sourceDirectory ** "*.scala.*").get.collect(templateExt).foreach {
          case (template, extension, t, format) =>
            val res = 
            ScalaTemplateCompiler.compile(
              template,
              sourceDirectory,
              generatedDir,
              t,
              format,
              additionalImports.map("import " + _.replace("%format%", extension)).mkString("\n"))
          
            println("res: "+res)
            res
        }
      } catch {
        case TemplateCompilationError(source, message, line, column) => {
          throw TemplateCompilationException(source, message, line, column - 1)
        }
        case e => throw e
      }

      (generatedDir ** "*.template.scala").get.map(_.getAbsoluteFile)
    }
  
  case class MyException(problems: Array[xsbti.Problem]) extends xsbti.CompileFailed {
    def arguments(): Array[String] = Array.empty
  }
  
  def improveErrorMsg(compileR: Result[inc.Analysis], streamManagerR: Result[Streams], streams: Result[TaskStreams]): inc.Analysis = compileR match {
    case Inc(incomplete) =>
      //sys.error("Big problem with: "+)
      val reporter = new LoggerReporter(10, streams.toEither.right.get.log)
      val probs = getProblems(incomplete, streamManagerR.toEither.right.get)
      println("Probs found: "+probs.size)
      probs.foreach { p =>
        val pos = p.position
        println("%s:%s" format (pos.line.get, pos.sourcePath.get))
        reporter.display(p.position, p.message, p.severity)
      }
      
      throw MyException(probs.toArray)
    case Value(v) => v
  }

    //(streamsManager flatMap (streams => sys.error(getProblems(error, streams).toString))).apply(t => t)
  //getProblems(error) {
  //  sys.error("Big problem with: "+error)
  //}

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
                    problem.position.offset.map { offset =>
                      generatedSource.mapPosition(offset.asInstanceOf[Int]) - IO.read(generatedSource.source.get).split('\n').take(problem.position.line.map(l => generatedSource.mapLine(l.asInstanceOf[Int])).get - 1).mkString("\n").size - 1
                    }.map { p =>
                      xsbti.Maybe.just(p.asInstanceOf[java.lang.Integer])
                    }.getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
                  }
                  def pointerSpace = xsbti.Maybe.just(" "*pointer.get)
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
            Incomplete.linearize(incomplete).filter(i => i.node.isDefined && i.node.get.isInstanceOf[ScopedKey[_]]).flatMap { i =>
              val JavacError = """\[error\]\s*(.*[.]java):(\d+):\s*(.*)""".r
              val JavacErrorInfo = """\[error\]\s*([a-z ]+):(.*)""".r
              val JavacErrorPosition = """\[error\](\s*)\^\s*""".r

              //Project.evaluateTask(streamsManager, state).get.toEither.right.toOption.map { streamsManager =>
              Some{
                var first: (Option[(String, String, String)], Option[Int]) = (None, None)
                var parsed: (Option[(String, String, String)], Option[Int]) = (None, None)
                Output.lastLines(i.node.get.asInstanceOf[ScopedKey[_]], streamsManager).map(_.replace(scala.Console.RESET, "")).map(_.replace(scala.Console.RED, "")).collect {
                  case JavacError(file, line, message) => parsed = Some((file, line, message)) -> None
                  case JavacErrorInfo(key, message) => parsed._1.foreach { o =>
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
                case (Some(error), maybePosition) => new xsbti.Problem {
                  def message = error._3
                  def position = new xsbti.Position {
                    def line = xsbti.Maybe.just(error._2.toInt)
                    def lineContent = IO.readLines(sourceFile.get)(line.get - 1)
                    def offset = xsbti.Maybe.nothing[java.lang.Integer]
                    def pointer = maybePosition.map(pos => xsbti.Maybe.just((pos - 1).asInstanceOf[java.lang.Integer])).getOrElse(xsbti.Maybe.nothing[java.lang.Integer])
                    def pointerSpace = xsbti.Maybe.just(" "*pointer.get)
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