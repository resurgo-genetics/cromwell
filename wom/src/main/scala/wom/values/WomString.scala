package wom.values

import wom.types.{WomStringType, WomType}

trait WomString extends WomValue {
  def value: String
}

// TODO CWL: This companion object should go away it's just a hack for PlaceholderExpression.
// Can't actually deprecate this since deprecations are warnings and warnings are errors but
// believe you me, this is deprecated.
// @deprecated(message = "TODO WOM", since = "Intro to WomTypes")
object WomString {
  def apply(string: String) = new WomString {
    override def value = string
    def valueString: String = ???

    def womType: wom.types.WomType = new WomStringType {
      override def arrayType = ???
      override def optionalType = ???
      override def isCoerceableFrom(other: WomType) = ???
      override def toDisplayString = ???
    }
  }
}
