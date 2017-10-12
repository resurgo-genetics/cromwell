package wom.types

trait WomArrayType extends WomType {
  def memberType: WomType
}
