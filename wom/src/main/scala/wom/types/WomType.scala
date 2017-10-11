package wom.types

trait WomType {

  /** Make an array type of the receiver type. e.g., if invoked on a WomString this will make a WomArrayType(WomString) */
  def arrayType: WomArrayType

  /** Make an optional type of the receiver type. e.g., if invoked on a WomString this will make a WomOptionalType(WomString) */
  def optionalType: WomOptionalType

  def isCoerceableFrom(other: WomType): Boolean

  def toDisplayString: String
}
