/* Copyright 2012 Typesafe (http://www.typesafe.com)
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

package twirl.compiler

import org.specs2.mutable._
import scalax.io.Resource

object TemplateParserSpec extends Specification {

  "The template parser" should {

    import scala.util.parsing.input.CharSequenceReader

    val parser = TwirlCompiler.templateParser

    def get(templateName: String) = {
      new CharSequenceReader(scalax.file.Path.fromString("twirl-compiler/src/test/templates/" + templateName).string)
    }

    def parse(templateName: String) = {
      parser.parser(get(templateName))
    }

    def parseString(template: String) = parser.parser(new CharSequenceReader(template))

    def parseStringSuccess(template: String) = parseString(template) must beLike {
      case parser.Success(_, rest) if rest.atEnd => ok
    }

    "succeed for" in {

      "static.scala.html" in {
        parse("static.scala.html") must beLike({ case parser.Success(_, rest) => if (rest.atEnd) ok else ko })
      }

      "simple.scala.html" in {
        parse("simple.scala.html") must beLike({ case parser.Success(_, rest) => if (rest.atEnd) ok else ko })
      }

      "complicated.scala.html" in {
        parse("complicated.scala.html") must beLike({ case parser.Success(_, rest) => if (rest.atEnd) ok else ko })
      }

      "brackets in strings" in {
        "open" in parseStringSuccess("""@foo("(")""")
        "close" in parseStringSuccess("""@foo(")@")""")
      }
    }

    "fail for" in {

      "unclosedBracket.scala.html" in {
        parse("unclosedBracket.scala.html") must beLike({
          case parser.NoSuccess(msg, rest) => {
            if (msg == "Unmatched bracket" && rest.pos.line == 8 && rest.pos.column == 12) ok else ko
          }
        })
      }

      "unclosedBracket2.scala.html" in {
        parse("unclosedBracket2.scala.html") must beLike({
          case parser.NoSuccess(msg, rest) => {
            if (msg == "Unmatched bracket" && rest.pos.line == 13 && rest.pos.column == 20) ok else ko
          }
        })
      }

      "invalidAt.scala.html" in {
        parse("invalidAt.scala.html") must beLike({
          case parser.NoSuccess(msg, rest) =>
            if (msg.contains("identifier' expected but `<' found") && rest.pos.line == 5 && rest.pos.column == 6) ok else ko
        })
      }
    }
  }
}