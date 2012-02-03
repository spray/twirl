package play.api.templates

trait Format[T <: Appendable[T]] {
  def raw(text: String): T
  def escape(text: String): T
}
