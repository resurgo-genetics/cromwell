package wom.values

trait WomString extends WomValue {
  def value: String
}

// TODO CWL: This companion object should go away it's just a hack for PlaceholderExpression.
@deprecated(message = "TODO WOM", since = "Intro to WomTypes")
object WomString {
  def apply(string: String) = new WomString { override def value = string }
}
