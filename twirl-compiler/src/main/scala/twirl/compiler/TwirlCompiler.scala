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
 * 2012-02-03 This class is taken almost verbatim from the Play framework.
 *            The runtime bits were moved into its own module and
 *            the import statements adapted accordingly.
 */

package twirl.compiler {

  import scalax.file._
  import java.io.File
  import scala.annotation.tailrec
import java.nio.charset.Charset
import scalax.io.Codec

object Hash {

    def apply(bytes: Array[Byte]) = {
      import java.security.MessageDigest
      val digest = MessageDigest.getInstance("SHA-1")
      digest.reset()
      digest.update(bytes)
      digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
    }

  }

  case class TemplateCompilationError(source: File, message: String, line: Int, column: Int) extends RuntimeException(message)

  object MaybeGeneratedSource {

    def unapply(source: File) = {
      val generated = GeneratedSource(source)
      if (generated.meta.isDefinedAt("SOURCE")) {
        Some(generated)
      } else {
        None
      }
    }

  }

  case class GeneratedSource(file: File) {

    lazy val meta: Map[String, String] = {
      val Meta = """([A-Z]+): (.*)""".r
      val UndefinedMeta = """([A-Z]+):""".r
      Map.empty[String, String] ++ {
        try {
          Path(file).string.split("-- GENERATED --")(1).trim.split('\n').map { m =>
            m.trim match {
              case Meta(key, value) => (key -> value)
              case UndefinedMeta(key) => (key -> "")
              case _ => ("UNDEFINED", "")
            }
          }.toMap
        } catch {
          case _ => Map.empty[String, String]
        }
      }
    }

    lazy val matrix: Seq[(Int, Int)] = {
      for (pos <- meta("MATRIX").split('|'); val c = pos.split("->"))
        yield try {
        Integer.parseInt(c(0)) -> Integer.parseInt(c(1))
      } catch {
        case _ => (0, 0) // Skip if MATRIX meta is corrupted
      }
    }

    lazy val lines: Seq[(Int, Int)] = {
      for (pos <- meta("LINES").split('|'); val c = pos.split("->"))
        yield try {
        Integer.parseInt(c(0)) -> Integer.parseInt(c(1))
      } catch {
        case _ => (0, 0) // Skip if LINES meta is corrupted
      }
    }

    def needRecompilation = (!file.exists ||
      // A generated source already exist but
      source.isDefined && ((source.get.lastModified > file.lastModified) || // the source has been modified
        (meta("HASH") != Hash(Path(source.get).byteArray))) // or the hash don't match
    )

    def mapPosition(generatedPosition: Int) = {
      matrix.indexWhere(p => p._1 > generatedPosition) match {
        case 0 => 0
        case i if i > 0 => {
          val pos = matrix(i - 1)
          pos._2 + (generatedPosition - pos._1)
        }
        case _ => {
          val pos = matrix.takeRight(1)(0)
          pos._2 + (generatedPosition - pos._1)
        }
      }
    }

    def mapLine(generatedLine: Int) = {
      lines.indexWhere(p => p._1 > generatedLine) match {
        case 0 => 0
        case i if i > 0 => {
          val line = lines(i - 1)
          line._2 + (generatedLine - line._1)
        }
        case _ => {
          val line = lines.takeRight(1)(0)
          line._2 + (generatedLine - line._1)
        }
      }
    }

    def toSourcePosition(marker: Int): (Int, Int) = {
      try {
        val targetMarker = mapPosition(marker)
        val line = Path(source.get).string.substring(0, targetMarker).split('\n').size
        (line, targetMarker)
      } catch {
        case _ => (0, 0)
      }
    }

    def source: Option[File] = {
      val s = new File(meta("SOURCE"))
      if (s == null || !s.exists) {
        None
      } else {
        Some(s)
      }
    }

    def sync() {
      if (file.exists && !source.isDefined) {
        file.delete()
      }
    }

  }

  object TwirlCompiler {

    import scala.util.parsing.input.Positional
    import scala.util.parsing.input.CharSequenceReader
    import scala.util.parsing.combinator.JavaTokenParsers

    abstract class TemplateTree
    abstract class ScalaExpPart

    case class Params(code: String) extends Positional
    case class Template(name: PosString, comment: Option[Comment], params: PosString, imports: Seq[Simple], defs: Seq[Def], sub: Seq[Template], content: Seq[TemplateTree]) extends Positional
    case class PosString(str: String) extends Positional {
      override def toString = str
    }
    case class Def(name: PosString, params: PosString, code: Simple) extends Positional
    case class Plain(text: String) extends TemplateTree with Positional
    case class Display(exp: ScalaExp) extends TemplateTree with Positional
    case class Comment(msg: String) extends TemplateTree with Positional
    case class ScalaExp(parts: Seq[ScalaExpPart]) extends TemplateTree with Positional
    case class Simple(code: String) extends ScalaExpPart with Positional
    case class Block(whitespace: String, args: Option[String], content: Seq[TemplateTree]) extends ScalaExpPart with Positional
    case class Value(ident: PosString, block: Block) extends Positional

    def compile(source: File, sourceDirectory: File, generatedDirectory: File, resultType: String,
                formatterType: String, sourceCharset: Charset, additionalImports: String = "",
                logRecompilation: File => Unit = _ => ()) = {
      val (templateName, generatedSource) = generatedFile(source, sourceDirectory, generatedDirectory)
      if (generatedSource.needRecompilation) {
        logRecompilation(generatedSource.file)
        implicit val codec = Codec(sourceCharset) // for reading twirl sources as well as writing .scala files
        val generated = templateParser.parser(new CharSequenceReader(Path(source).string)) match {
          case templateParser.Success(parsed, rest) if rest.atEnd => {
            generateFinalTemplate(
              source,
              templateName.dropRight(1).mkString("."),
              templateName.takeRight(1).mkString,
              parsed,
              resultType,
              formatterType,
              additionalImports)
          }
          case templateParser.Success(_, rest) => {
            throw new TemplateCompilationError(source, "Not parsed?", rest.pos.line, rest.pos.column)
          }
          case templateParser.NoSuccess(message, input) => {
            throw new TemplateCompilationError(source, message, input.pos.line, input.pos.column)
          }
        }
        Path(generatedSource.file).write(generated)
        Some(generatedSource.file)
      } else {
        None
      }
    }

    def generatedFile(template: File, sourceDirectory: File, generatedDirectory: File) = {
      val templateName = source2TemplateName(template, sourceDirectory, template.getName.split('.').takeRight(1).head)
      templateName -> GeneratedSource(new File(generatedDirectory, templateName.mkString("/") + ".template.scala"))
    }

    def source2TemplateName(f: File, sourceDirectory: File, Ext: String, suffix: String = ""): Seq[String] = {
      val canonicalSource = sourceDirectory.getCanonicalFile
      def parentDirs(cur: File): List[String] =
        if (cur == canonicalSource) Nil else cur.getName :: parentDirs(cur.getParentFile)
      val parents = parentDirs(f.getCanonicalFile.getParentFile).reverse
      val Name = """([a-zA-Z0-9_]+)[.]scala[.]([a-z]+)""".r
      val Name(name, Ext) = f.getName
      parents :+ Ext :+ name
    }

    val templateParser = new JavaTokenParsers {

      def as[T](parser: Parser[T], error: String) = {
        Parser(in => parser(in) match {
          case s @ Success(_, _) => s
          case Failure(_, next) => Failure("`" + error + "' expected but `" + next.first + "' found", next)
          case Error(_, next) => Error(error, next)
        })
      }

      def several[T](p: => Parser[T]): Parser[List[T]] = Parser { in =>
        import scala.collection.mutable.ListBuffer
        val elems = new ListBuffer[T]
        def continue(in: Input): ParseResult[List[T]] = {
          val p0 = p // avoid repeatedly re-evaluating by-name parser
          @tailrec
          def applyp(in0: Input): ParseResult[List[T]] = p0(in0) match {
            case Success(x, rest) => elems += x; applyp(rest)
            case Failure(_, _) => Success(elems.toList, in0)
            case err: Error => err
          }
          applyp(in)
        }
        continue(in)
      }

      def at = "@"

      def eof = """\Z""".r

      def newLine = (("\r"?) ~> "\n")

      def identifier = as(ident, "identifier")

      def whiteSpaceNoBreak = """[ \t]+""".r

      def escapedAt = at ~> at

      def any = {
        Parser(in => if (in.atEnd) {
          Failure("end of file", in)
        } else {
          Success(in.first, in.rest)
        })
      }

      def plain: Parser[Plain] = {
        positioned(
          ((escapedAt | (not(at) ~> (not("{" | "}") ~> any))) +) ^^ {
            case charList => Plain(charList.mkString)
          })
      }

      def squareBrackets: Parser[String] = {
        "[" ~ (several((squareBrackets | not("]") ~> any))) ~ commit("]") ^^ {
          case p1 ~ charList ~ p2 => p1 + charList.mkString + p2
        }
      }

      def parentheses: Parser[String] = {
        "(" ~ (several((parentheses | not(")") ~> any))) ~ commit(")") ^^ {
          case p1 ~ charList ~ p2 => p1 + charList.mkString + p2
        }
      }

      def comment: Parser[Comment] = {
        (at ~ "*") ~> ((not("*@") ~> any *) ^^ { case chars => Comment(chars.mkString) }) <~ ("*" ~ at)
      }

      def brackets: Parser[String] = {
        ensureMatchedBrackets((several((brackets | not("}") ~> any)))) ^^ {
          case charList => "{" + charList.mkString + "}"
        }
      }

      def ensureMatchedBrackets[T](p: Parser[T]): Parser[T] = Parser { in =>
        val pWithBrackets = "{" ~> p <~ ("}" | eof ~ err("EOF"))
        pWithBrackets(in) match {
          case s @ Success(_, _) => s
          case f @ Failure(_, _) => f
          case Error("EOF", _) => Error("Unmatched bracket", in)
          case e: Error => e
        }
      }

      def block: Parser[Block] = {
        positioned(
          (whiteSpaceNoBreak?) ~ ensureMatchedBrackets((blockArgs?) ~ several(mixed)) ^^ {
            case w ~ (args ~ content) => Block(w.getOrElse(""), args, content.flatten)
          })
      }

      def blockArgs: Parser[String] = (not("=>" | newLine) ~> any *) ~ "=>" ^^ { case args ~ arrow => args.mkString + arrow }

      def methodCall: Parser[String] = identifier ~ (squareBrackets?) ~ (parentheses?) ^^ {
        case methodName ~ types ~ args => methodName + types.getOrElse("") + args.getOrElse("")
      }

      def expression: Parser[Display] = {
        at ~> commit(positioned(methodCall ^^ { case code => Simple(code) })) ~ several(expressionPart) ^^ {
          case first ~ parts => Display(ScalaExp(first :: parts))
        }
      }

      def expressionPart: Parser[ScalaExpPart] = {
        chainedMethods | block | (whiteSpaceNoBreak ~> scalaBlockChained) | elseCall | (parentheses ^^ { case code => Simple(code) })
      }

      def chainedMethods: Parser[Simple] = {
        positioned(
          "." ~> rep1sep(methodCall, ".") ^^ {
            case calls => Simple("." + calls.mkString("."))
          })
      }

      def elseCall: Parser[Simple] = {
        (whiteSpaceNoBreak?) ~> positioned("else" ^^ { case e => Simple(e) }) <~ (whiteSpaceNoBreak?)
      }

      def safeExpression: Parser[Display] = {
        at ~> positioned(parentheses ^^ { case code => Simple(code) }) ^^ {
          case code => Display(ScalaExp(code :: Nil))
        }
      }

      def matchExpression: Parser[Display] = {
        at ~> positioned(identifier ~ whiteSpaceNoBreak ~ "match" ^^ { case i ~ w ~ m => Simple(i + w + m) }) ~ block ^^ {
          case expr ~ block => {
            Display(ScalaExp(List(expr, block)))
          }
        }
      }

      def forExpression: Parser[Display] = {
        at ~> positioned("for" ~ parentheses ^^ { case f ~ p => Simple(f + p + " yield ") }) ~ block ^^ {
          case expr ~ block => {
            Display(ScalaExp(List(expr, block)))
          }
        }
      }

      def caseExpression: Parser[ScalaExp] = {
        (whiteSpace?) ~> positioned("""case (.+)=>""".r ^^ { case c => Simple(c) }) ~ block <~ (whiteSpace?) ^^ {
          case pattern ~ block => ScalaExp(List(pattern, block))
        }
      }

      def importExpression: Parser[Simple] = {
        positioned(
          at ~> """import .*(\r)?\n""".r ^^ {
            case stmt => Simple(stmt)
          })
      }

      def scalaBlock: Parser[Simple] = {
        at ~> positioned(
          brackets ^^ { case code => Simple(code) })
      }

      def scalaBlockChained: Parser[Block] = {
        scalaBlock ^^ {
          case code => Block("", None, ScalaExp(code :: Nil) :: Nil)
        }
      }

      def scalaBlockDisplayed: Parser[Display] = {
        scalaBlock ^^ {
          case code => Display(ScalaExp(code :: Nil))
        }
      }

      def mixed: Parser[Seq[TemplateTree]] = {
        ((comment | scalaBlockDisplayed | caseExpression | matchExpression | forExpression | safeExpression | plain | expression) ^^ { case t => List(t) }) |
          ("{" ~ several(mixed) ~ "}") ^^ { case p1 ~ content ~ p2 => Plain(p1) +: content.flatten :+ Plain(p2) }
      }

      def template: Parser[Template] = {
        templateDeclaration ~ """[ \t]*=[ \t]*[{]""".r ~ templateContent <~ "}" ^^ {
          case declaration ~ assign ~ content => {
            Template(declaration._1, None, declaration._2, content._1, content._2, content._3, content._4)
          }
        }
      }

      def localDef: Parser[Def] = {
        templateDeclaration ~ """[ \t]*=[ \t]*""".r ~ scalaBlock ^^ {
          case declaration ~ w ~ code => {
            Def(declaration._1, declaration._2, code)
          }
        }
      }

      def templateDeclaration: Parser[(PosString, PosString)] = {
        at ~> positioned(identifier ^^ { case s => PosString(s) }) ~ positioned(opt(squareBrackets) ~ several(parentheses) ^^ { case t ~ p => PosString(t.getOrElse("") + p.mkString) }) ^^ {
          case name ~ params => name -> params
        }
      }

      def templateContent: Parser[(List[Simple], List[Def], List[Template], List[TemplateTree])] = {
        (several(importExpression | localDef | template | mixed)) ^^ {
          case elems => {
            elems.foldLeft((List[Simple](), List[Def](), List[Template](), List[TemplateTree]())) { (s, e) =>
              e match {
                case i: Simple => (s._1 :+ i, s._2, s._3, s._4)
                case d: Def => (s._1, s._2 :+ d, s._3, s._4)
                case v: Template => (s._1, s._2, s._3 :+ v, s._4)
                case c: Seq[_] => (s._1, s._2, s._3, s._4 ++ c.asInstanceOf[Seq[TemplateTree]])
              }
            }
          }
        }
      }

      def parser: Parser[Template] = {
        opt(comment) ~ opt(whiteSpace) ~ opt(at ~> positioned((parentheses+) ^^ { case s => PosString(s.mkString) })) ~ templateContent ^^ {
          case comment ~ _ ~ args ~ content => {
            Template(PosString(""), comment, args.getOrElse(PosString("()")), content._1, content._2, content._3, content._4)
          }
        }
      }

      override def skipWhitespace = false

    }

    def visit(elem: Seq[TemplateTree], previous: Seq[Any]): Seq[Any] = {
      elem match {
        case head :: tail =>
          val tripleQuote = "\"\"\""
          visit(tail, head match {
            case p @ Plain(text) => (if (previous.isEmpty) Nil else previous :+ ",") :+ "format.raw" :+ Source("(", p.pos) :+ tripleQuote :+ text :+ tripleQuote :+ ")"
            case Comment(msg) => previous
            case Display(exp) => (if (previous.isEmpty) Nil else previous :+ ",") :+ "_display_(Seq(" :+ visit(Seq(exp), Nil) :+ "))"
            case ScalaExp(parts) => previous :+ parts.map {
              case s @ Simple(code) => Source(code, s.pos)
              case b @ Block(whitespace, args, content) if (content.forall(_.isInstanceOf[ScalaExp])) => Nil :+ Source(whitespace + "{" + args.getOrElse(""), b.pos) :+ visit(content, Nil) :+ "}"
              case b @ Block(whitespace, args, content) => Nil :+ Source(whitespace + "{" + args.getOrElse(""), b.pos) :+ "_display_(Seq(" :+ visit(content, Nil) :+ "))}"
            }
          })
        case Nil => previous
      }
    }

    def templateCode(template: Template, resultType: String): Seq[Any] = {

      val defs = (template.sub ++ template.defs).map { i =>
        i match {
          case t: Template if t.name == "" => templateCode(t, resultType)
          case t: Template => {
            Nil :+ (if (t.name.str.startsWith("implicit")) "implicit def " else "def ") :+ Source(t.name.str, t.name.pos) :+ Source(t.params.str, t.params.pos) :+ ":" :+ resultType :+ " = {_display_(" :+ templateCode(t, resultType) :+ ")};"
          }
          case Def(name, params, block) => {
            Nil :+ (if (name.str.startsWith("implicit")) "implicit def " else "def ") :+ Source(name.str, name.pos) :+ Source(params.str, params.pos) :+ " = {" :+ block.code :+ "};"
          }
        }
      }

      val imports = template.imports.map(_.code).mkString("\n")

      Nil :+ imports :+ "\n" :+ defs :+ "\n" :+ "Seq(" :+ visit(template.content, Nil) :+ ")"
    }

    def generateFinalTemplate(template: File, packageName: String, name: String, root: Template, resultType: String, formatterType: String, additionalImports: String) = {

      val extra = TemplateAsFunctionCompiler.getFunctionMapping(
        root.params.str,
        resultType)

      val generated = {
        Nil :+ """
package """ :+ packageName :+ """

import twirl.api._
import TemplateMagic._

""" :+ additionalImports :+ """
/*""" :+ root.comment.map(_.msg).getOrElse("") :+ """*/
object """ :+ name :+ """ extends BaseScalaTemplate[""" :+ resultType :+ """,Format[""" :+ resultType :+ """]](""" :+ formatterType :+ """) with """ :+ extra._3 :+ """ {

    /*""" :+ root.comment.map(_.msg).getOrElse("") :+ """*/
    def apply""" :+ Source(root.params.str, root.params.pos) :+ """:""" :+ resultType :+ """ = {
        _display_ {""" :+ templateCode(root, resultType) :+ """}
    }

    """ :+ extra._1 :+ """

    """ :+ extra._2 :+ """

    def ref = this

}"""
      }

      Source.finalSource(template, generated)
    }

    object TemplateAsFunctionCompiler {

      // Note, the presentation compiler is not thread safe, all access to it must be synchronized.  If access to it
      // is not synchronized, then weird things happen like FreshRunReq exceptions are thrown when multiple sub projects
      // are compiled (done in parallel by default by SBT).  So if adding any new methods to this object, make sure you
      // make them synchronized.

      import java.io.File
      import scala.tools.nsc.interactive.{ Response, Global }
      import scala.tools.nsc.io.AbstractFile
      import scala.tools.nsc.util.{ SourceFile, Position, BatchSourceFile }
      import scala.tools.nsc.Settings
      import scala.tools.nsc.reporters.ConsoleReporter

      def getFunctionMapping(signature: String, returnType: String) = synchronized {

        type Tree = PresentationCompiler.global.Tree
        type DefDef = PresentationCompiler.global.DefDef
        type TypeDef = PresentationCompiler.global.TypeDef

        def filterType(t: String) = t match {
          case vararg if vararg.startsWith("_root_.scala.<repeated>") => vararg.replace("_root_.scala.<repeated>", "Array")
          case synthetic if synthetic.contains("<synthetic>") => synthetic.replace("<synthetic>", "")
          case t => t
        }

        def findSignature(tree: Tree): Option[DefDef] = {
          tree match {
            case t: DefDef if t.name.toString == "signature" => Some(t)
            case t: Tree => t.children.flatMap(findSignature).headOption
          }
        }

        val params = findSignature(
          PresentationCompiler.treeFrom("object FT { def signature" + signature + " }")).get.vparamss

        val functionType = "(" + params.map(group => "(" + group.map {
          case a if a.mods.isByNameParam => " => " + a.tpt.children(1).toString
          case a => filterType(a.tpt.toString)
        }.mkString(",") + ")").mkString(" => ") + " => " + returnType + ")"

        val renderCall = "def render%s = apply%s".format(
          "(" + params.flatten.map {
            case a if a.mods.isByNameParam => a.name.toString + ":" + a.tpt.children(1).toString
            case a => a.name.toString + ":" + filterType(a.tpt.toString)
          }.mkString(",") + ")",
          params.map(group => "(" + group.map { p =>
            p.name.toString + Option(p.tpt.toString).filter(_.startsWith("_root_.scala.<repeated>")).map(_ => ":_*").getOrElse("")
          }.mkString(",") + ")").mkString)

        var templateType = "twirl.api.Template%s[%s%s]".format(
          params.flatten.size,
          params.flatten.map {
            case a if a.mods.isByNameParam => a.tpt.children(1).toString
            case a => filterType(a.tpt.toString)
          }.mkString(","),
          (if (params.flatten.isEmpty) "" else ",") + returnType)

        val f = "def f:%s = %s => apply%s".format(
          functionType,
          params.map(group => "(" + group.map(_.name.toString).mkString(",") + ")").mkString(" => "),
          params.map(group => "(" + group.map { p =>
            p.name.toString + Option(p.tpt.toString).filter(_.startsWith("_root_.scala.<repeated>")).map(_ => ":_*").getOrElse("")
          }.mkString(",") + ")").mkString)

        (renderCall, f, templateType)
      }

      class CompilerInstance {

        def additionalClassPathEntry: Option[String] = None

        lazy val compiler = {

          val settings = new Settings

          val scalaObjectSource = Class.forName("scala.ScalaObject").getProtectionDomain.getCodeSource

          // is null in Eclipse/OSGI but luckily we don't need it there
          if (scalaObjectSource != null) {
            val compilerPath = Class.forName("scala.tools.nsc.Interpreter").getProtectionDomain.getCodeSource.getLocation.getFile
            val libPath = scalaObjectSource.getLocation.getFile
            val pathList = List(compilerPath, libPath)
            val origBootclasspath = settings.bootclasspath.value
            settings.bootclasspath.value = ((origBootclasspath :: pathList) ::: additionalClassPathEntry.toList) mkString File.pathSeparator
          }

          val compiler = new Global(settings, new ConsoleReporter(settings) {
            override def printMessage(pos: Position, msg: String) = ()
          })

          new compiler.Run

          compiler
        }
      }

      trait TreeCreationMethods {

        val global: scala.tools.nsc.interactive.Global

        val randomFileName = {
          val r = new java.util.Random
          () => "file" + r.nextInt
        }

        def treeFrom(src: String): global.Tree = {
          val file = new BatchSourceFile(randomFileName(), src)
          treeFrom(file)
        }

        def treeFrom(file: SourceFile): global.Tree = {
          import tools.nsc.interactive.Response

          type Scala29Compiler = {
            def askParsedEntered(file: SourceFile, keepLoaded: Boolean, response: Response[global.Tree]): Unit
            def askType(file: SourceFile, forceReload: Boolean, respone: Response[global.Tree]): Unit
          }

          val newCompiler = global.asInstanceOf[Scala29Compiler]

          val r1 = new Response[global.Tree]
          newCompiler.askParsedEntered(file, true, r1)
          r1.get.left.get
        }

      }

      object CompilerInstance extends CompilerInstance

      object PresentationCompiler extends TreeCreationMethods {
        val global = CompilerInstance.compiler

        def shutdown() {
          global.askShutdown()
        }
      }

    }

  }

  /* ------- */

  import scala.util.parsing.input.{ Position, OffsetPosition, NoPosition }

  case class Source(code: String, pos: Position = NoPosition)

  object Source {

    import scala.collection.mutable.ListBuffer

    def finalSource(template: File, generatedTokens: Seq[Any]) = {
      val scalaCode = new StringBuilder
      val positions = ListBuffer.empty[(Int, Int)]
      val lines = ListBuffer.empty[(Int, Int)]
      serialize(generatedTokens, scalaCode, positions, lines)
      scalaCode + """
                /*
                    -- GENERATED --
                    DATE: """ + new java.util.Date + """
                    SOURCE: """ + template.getAbsolutePath.replace(File.separator, "/") + """
                    HASH: """ + Hash(Path(template).byteArray) + """
                    MATRIX: """ + positions.map { pos =>
        pos._1 + "->" + pos._2
      }.mkString("|") + """
                    LINES: """ + lines.map { line =>
        line._1 + "->" + line._2
      }.mkString("|") + """
                    -- GENERATED --
                */
            """
    }

    private def serialize(parts: Seq[Any], source: StringBuilder, positions: ListBuffer[(Int, Int)], lines: ListBuffer[(Int, Int)]) {
      parts.foreach {
        case s: String => source.append(s)
        case Source(code, pos @ OffsetPosition(_, offset)) => {
          source.append("/*" + pos + "*/")
          positions += (source.length -> offset)
          lines += (source.toString.split('\n').size -> pos.line)
          source.append(code)
        }
        case Source(code, NoPosition) => source.append(code)
        case s: Seq[any] => serialize(s, source, positions, lines)
      }
    }

  }
}
