package wdl

import wom.types.{WdlOptionalType, WdlType}

case class WorkflowInput(fqn: FullyQualifiedName, wdlType: WdlType) {
  val optional = wdlType.isInstanceOf[WdlOptionalType]
}
