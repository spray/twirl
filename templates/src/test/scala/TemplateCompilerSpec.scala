package play.templates.test

import org.specs2.mutable._
import play.templates._
import play.api.templates.{Appendable, Format}

import java.io._

object TemplateCompilerSpec extends Specification {

  import Helper._

  val sourceDir = new File("templates/src/test/templates").getCanonicalFile
  val generatedDir = new File("templates/target/scala-2.9.1/test/generated-templates").getCanonicalFile
  val generatedClasses = new File("templates/target/scala-2.9.1/test/generated-classes").getCanonicalFile
  scalax.file.Path(generatedDir).deleteRecursively()
  scalax.file.Path(generatedClasses).deleteRecursively()
  scalax.file.Path(generatedClasses).createDirectory()

  "The template compiler" should {
    "compile successfully" in {
      val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)
      helper.compile[((String, List[String]) => (Int) => Html)]("real.scala.html", "html.real")("World", List("A", "B"))(4).toString.trim must beLike {
        case html =>
          {
            if (html.contains("<h1>Hello World</h1>") &&
              html.contains("You have 2 items") &&
              html.contains("EA") &&
              html.contains("EB")) ok else ko
          }
      }

      helper.compile[(() => Html)]("static.scala.html", "html.static")().toString.trim must be_==(
        "<h1>It works</h1>")

      val hello = helper.compile[((String) => Html)]("hello.scala.html", "html.hello")("World").toString.trim
      
      hello must be_==(
        "<h1>Hello World!</h1><h1>xml</h1>")
          
      helper.compile[((collection.immutable.Set[String]) => Html)]("set.scala.html", "html.set")(Set("first","second","third")).toString.trim.replace("\n","").replaceAll("\\s+", "") must be_==("firstsecondthird")   

    }
    "fail compilation for error.scala.html" in {
      val helper = new CompilerHelper(sourceDir, generatedDir, generatedClasses)
      helper.compile[(() => Html)]("error.scala.html", "html.error") must throwA[CompilationError].like {
        case CompilationError(_, 2, 12) => ok
        case _ => ko
      }
    }

  }

}

object Helper {

  case class Html(text: String) extends Appendable[Html] {
    val buffer = new StringBuilder(text)
    def +(other: Html) = {
      buffer.append(other.buffer)
      this
    }
    override def toString = buffer.toString
  }

  object HtmlFormat extends Format[Html] {
    def raw(text: String) = Html(text)
    def escape(text: String) = Html(text.replace("<", "&lt;"))
  }

  case class CompilationError(message: String, line: Int, column: Int) extends RuntimeException(message)

  class CompilerHelper(sourceDir: File, generatedDir: File, generatedClasses: File) {
    import scala.tools.nsc.interactive.{ Response, Global }
    import scala.tools.nsc.io.AbstractFile
    import scala.tools.nsc.util.{ SourceFile, Position, BatchSourceFile }
    import scala.tools.nsc.Settings
    import scala.tools.nsc.reporters.ConsoleReporter

    import java.net._

    val templateCompiler = ScalaTemplateCompiler

    val classloader = new URLClassLoader(Array(generatedClasses.toURI.toURL), Class.forName("play.templates.ScalaTemplateCompiler").getClassLoader)

    val compiler = {

      def additionalClassPathEntry: Seq[String] =
        Class.forName("play.templates.ScalaTemplateCompiler").getClassLoader.asInstanceOf[URLClassLoader].getURLs.map(_.getFile).map(_.toString)

      val settings = new Settings
      val scalaObjectSource = Class.forName("scala.ScalaObject").getProtectionDomain.getCodeSource

      // is null in Eclipse/OSGI but luckily we don't need it there
      if (scalaObjectSource != null) {
        val compilerPath = Class.forName("scala.tools.nsc.Interpreter").getProtectionDomain.getCodeSource.getLocation
        val libPath = scalaObjectSource.getLocation
        val pathList = List(compilerPath, libPath)
        val origBootclasspath = settings.bootclasspath.value

        def isTemplateCompiler(path: String): Boolean =
          path.endsWith("templates/target/scala-2.9.1/classes/")

        val fullClassPath = ((origBootclasspath :: pathList) ::: additionalClassPathEntry.toList) map (_.toString) filterNot isTemplateCompiler

        settings.bootclasspath.value = fullClassPath mkString File.pathSeparator
        settings.outdir.value = generatedClasses.getAbsolutePath
      }

      val compiler = new Global(settings, new ConsoleReporter(settings) {
        override def printMessage(pos: Position, msg: String) = {
          throw CompilationError(msg, pos.line, pos.point)
        }
      })

      new compiler.Run

      compiler
    }

    def compile[T](templateName: String, className: String): T = {
      val templateFile = new File(sourceDir, templateName)
      val Some(generated) = templateCompiler.compile(templateFile, sourceDir, generatedDir, "play.templates.test.Helper.Html", "play.templates.test.Helper.HtmlFormat")

      val mapper = GeneratedSource(generated)

      val run = new compiler.Run

      try {
        run.compile(List(generated.getAbsolutePath))
      } catch {
        case CompilationError(msg, line, column) => throw CompilationError(
          msg, mapper.mapLine(line), mapper.mapPosition(column))
      }

      val t = classloader.loadClass(className + "$").getDeclaredField("MODULE$").get(null)

      t.getClass.getDeclaredMethod("f").invoke(t).asInstanceOf[T]
    }
  }
}
