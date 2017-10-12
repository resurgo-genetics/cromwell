package wom.values

import wom.types.WomType

trait WomValue {
  def womType: WomType
  def valueString: String
}
