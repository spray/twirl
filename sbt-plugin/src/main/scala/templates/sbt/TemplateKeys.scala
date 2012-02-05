package templates.sbt

import java.io.File
import sbt._

trait TemplateKeys {
  val templatesImport = SettingKey[Seq[String]]("play-templates-imports", "Additional imports for the templates")
  val templatesTypes = SettingKey[PartialFunction[String, (String, String)]]("play-templates-formats", "Defined template formats")

  val compileTemplates = TaskKey[Seq[File]]("compile-templates", "Compile scala templates into scala files")
  val templatesReportErrors = TaskKey[inc.Analysis]("templates-report-errorrs", "Reports template errors")
}