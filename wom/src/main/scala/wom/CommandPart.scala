package wom

import wom.values.WdlValue
import wom.expression.IoFunctionSet
import wom.graph.LocalName

trait CommandPart {
  def instantiate(inputsMap: Map[LocalName, WdlValue],
                  functions: IoFunctionSet,
                  valueMapper: WdlValue => WdlValue): String
}
