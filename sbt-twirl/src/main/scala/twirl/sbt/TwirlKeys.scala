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

package twirl.sbt

import java.io.File
import sbt._
import java.nio.charset.Charset

trait TwirlKeys {

  val twirlImports = SettingKey[Seq[String]]("twirl-imports", "Additional imports available to the twirl templates")

  val twirlTemplatesTypes = SettingKey[TemplateTypeMap]("twirl-template-types", "Defined templates types")

  val twirlSourceCharset = SettingKey[Charset]("twirl-source-charset",
    "The charset to use when reading twirl sources and writing template .scala files")

  val twirlCompile = TaskKey[Seq[File]]("twirl-compile", "Compile twirl templates into scala source files")

  val twirlReportErrors = TaskKey[inc.Analysis]("twirl-report-errors", "Report twirl template errors")
}