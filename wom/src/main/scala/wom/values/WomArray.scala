package wom.values

import wom.types.{WomArrayType, WomType}

trait WomArray extends WomValue {

  def memberType: WomType = womType.asInstanceOf[WomArrayType].memberType

}
