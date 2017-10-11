package wom.types

import wom.values.WomOptionalValue

trait WomOptionalType extends WomType {
  def memberType: WomType
  def none: WomOptionalValue
}
