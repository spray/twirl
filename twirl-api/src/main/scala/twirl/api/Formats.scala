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

/**
 * Generic type representing content to be sent over an HTTP response.
 */
trait Content {

  /**
   * The content String.
   */
  def body: String

  /**
   * The default Content type to use for this content.
   */
  def contentType: String

}

/**
 * Content type used in default HTML templates.
 *
 * @param text the HTML text
 */
case class Html(text: String) extends Appendable[Html] with Content {
  val buffer = new StringBuilder(text)

  /**
   * Appends this HTML fragment to another.
   */
  def +(other: Html) = {
    buffer.append(other.buffer)
    this
  }

  override def toString = buffer.toString

  /**
   * Content type of HTML (`text/html`).
   */
  def contentType = "text/html"

  def body = toString

}

/**
 * Helper for HTML utility methods.
 */
object Html {

  /**
   * Creates an empty HTML fragment.
   */
  def empty = Html("")

}

/**
 * Formatter for HTML content.
 */
object HtmlFormat extends Format[Html] {

  /**
   * Creates a raw (unescaped) HTML fragment.
   */
  def raw(text: String) = Html(text)

  /**
   * Creates a safe (escaped) HTML fragment.
   */
  def escape(text: String) = Html(org.apache.commons.lang.StringEscapeUtils.escapeHtml(text))

}

/**
 * Content type used in default text templates.
 *
 * @param text The plain text.
 */
case class Txt(text: String) extends Appendable[Txt] with Content {
  val buffer = new StringBuilder(text)

  /**
   * Appends this text fragment to another.
   */
  def +(other: Txt) = {
    buffer.append(other.buffer)
    this
  }

  override def toString = buffer.toString

  /**
   * Content type of text (`text/plain`).
   */
  def contentType = "text/plain"

  def body = toString

}

/**
 * Helper for utilities Txt methods.
 */
object Txt {

  /**
   * Creates an empty text fragment.
   */
  def empty = Txt("")

}

/**
 * Formatter for text content.
 */
object TxtFormat extends Format[Txt] {

  /**
   * Create a text fragment.
   */
  def raw(text: String) = Txt(text)

  /**
   * No need for a safe (escaped) text fragment.
   */
  def escape(text: String) = Txt(text)

}

/**
 * Content type used in default XML templates.
 *
 * @param text the plain xml text
 */
case class Xml(text: String) extends Appendable[Xml] with Content {
  val buffer = new StringBuilder(text)

  /**Append this XML fragment to another. */
  def +(other: Xml) = {
    buffer.append(other.buffer)
    this
  }

  override def toString = buffer.toString

  /**
   * Content type of XML (`text/xml`).
   */
  def contentType = "text/xml"

  def body = toString

}

/**
 * Helper for XML utility methods.
 */
object Xml {

  /**
   * Create an empty XML fragment.
   */
  def empty = Xml("")

}

/**
 * Formatter for XML content.
 */
object XmlFormat extends Format[Xml] {

  /**
   * Creates an XML fragment.
   */
  def raw(text: String) = Xml(text)

  /**
   * Creates an escaped XML fragment.
   */
  def escape(text: String) = Xml(org.apache.commons.lang.StringEscapeUtils.escapeXml(text))

}