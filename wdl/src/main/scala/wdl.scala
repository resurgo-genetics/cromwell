
import wdl.exception.OutputVariableLookupException
import wom.values.WomValue

import scala.util.{Failure, Try}

package object wdl {

  type OutputResolver = (WdlGraphNode, Option[Int]) => Try[WomValue]

  val NoOutputResolver: OutputResolver = (node: WdlGraphNode, i: Option[Int]) => Failure(OutputVariableLookupException(node, i))
}
