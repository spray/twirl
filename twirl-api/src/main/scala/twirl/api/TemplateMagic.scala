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

package twirl.api

object TemplateMagic {

  // --- UTILS

  def defining[T](t: T)(handler: T => Any) = {
    handler(t)
  }

  def using[T](t: T) = t

  // --- IF

  implicit def iterableToBoolean(x: Iterable[_]) = x != null && !x.isEmpty

  implicit def optionToBoolean(x: Option[_]) = x != null && x.isDefined

  implicit def stringToBoolean(x: String) = x != null && !x.isEmpty

  // --- JAVA

  implicit def javaCollectionToScala[T](x: java.lang.Iterable[T]) = {
    import scala.collection.JavaConverters._
    x.asScala
  }

  // --- DEFAULT

  case class Default(default: Any) {
    def ?:(x: Any) = x match {
      case "" => default
      case Nil => default
      case false => default
      case 0 => default
      case None => default
      case _ => x
    }
  }

  implicit def anyToDefault(x: Any) = Default(x)

  // --- DATE

  class RichDate(date: java.util.Date) {

    def format(pattern: String) = {
      new java.text.SimpleDateFormat(pattern).format(date)
    }

  }

  implicit def richDate(date: java.util.Date) = new RichDate(date)

  // --- STRING

  class RichString(string: String) {

    def when(predicate: => Boolean) = {
      predicate match {
        case true => string
        case false => ""
      }
    }

  }

  implicit def richString(string: String) = new RichString(string)

}