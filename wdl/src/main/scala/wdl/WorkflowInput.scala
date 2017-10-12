package wdl

import wdl.types.{WdlOptionalType, WdlType}
import wom.FullyQualifiedName

case class WorkflowInput(fqn: FullyQualifiedName, wdlType: WdlType) {
  val optional = wdlType.isInstanceOf[WdlOptionalType]
}
