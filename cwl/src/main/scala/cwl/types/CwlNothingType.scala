package cwl.types

import wom.types.{WomNothingType, WomType}

object CwlNothingType extends WomNothingType with CwlType {

  override def isCoerceableFrom(other: WomType) = ???

  override def toDisplayString = ???
}
