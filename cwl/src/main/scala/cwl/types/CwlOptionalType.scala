package cwl.types

import wom.types.{WomOptionalType, WomType}

case class CwlOptionalType(memberType: WomType) extends WomOptionalType with CwlType {

  override def none = ???

  override def isCoerceableFrom(other: WomType) = ???

  override def toDisplayString = ???
}
