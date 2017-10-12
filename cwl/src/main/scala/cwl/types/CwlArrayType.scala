package cwl.types

import wom.types.{WomArrayType, WomType}

case class CwlArrayType(memberType: WomType) extends WomArrayType with CwlType {

  override def isCoerceableFrom(other: WomType) = ???

  override def toDisplayString = ???
}
