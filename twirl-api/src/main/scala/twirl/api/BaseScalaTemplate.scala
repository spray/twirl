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

case class BaseScalaTemplate[T <: Appendable[T], F <: Format[T]](format: F) {

  def _display_(o: Any)(implicit m: Manifest[T]): T = {
    o match {
      case escaped if escaped != null && escaped.getClass == m.erasure => escaped.asInstanceOf[T]
      case () => format.raw("")
      case None => format.raw("")
      case Some(v) => _display_(v)
      case xml: scala.xml.NodeSeq => format.raw(xml.toString)
      case escapeds: TraversableOnce[_] => escapeds.foldLeft(format.raw(""))(_ + _display_(_))
      case escapeds: Array[_] => escapeds.foldLeft(format.raw(""))(_ + _display_(_))
      case string: String => format.escape(string)
      case v if v != null => _display_(v.toString)
      case _ => format.raw("")
    }
  }

}