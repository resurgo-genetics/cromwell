package cwl.types

import wom.types.{WomStringType, WomType}

object CwlStringType extends WomStringType with CwlType {

  override def isCoerceableFrom(other: WomType) = ???

  override def toDisplayString = ???
}
