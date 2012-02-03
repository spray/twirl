package play.api.templates

trait Appendable[T] {
  def +(other: T): T
  override def equals(x: Any): Boolean = super.equals(x)
  override def hashCode() = super.hashCode()
}
