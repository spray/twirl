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

package templates.sbt

import java.io.File
import sbt._

trait TemplateKeys {
  val templatesImport = SettingKey[Seq[String]]("templates-imports", "Additional imports for the templates")
  val templatesTypes = SettingKey[PartialFunction[String, (String, String)]]("templates-formats", "Defined template formats")

  val compileTemplates = TaskKey[Seq[File]]("compile-templates", "Compile scala templates into scala files")
  val templatesReportErrors = TaskKey[inc.Analysis]("templates-report-errorrs", "Reports template errors")
}