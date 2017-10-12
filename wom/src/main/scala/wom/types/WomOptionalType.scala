package wom.types

import wom.values.WomOptional

trait WomOptionalType extends WomType {
  def memberType: WomType
  def none: WomOptional
}
