package play.api.templates

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