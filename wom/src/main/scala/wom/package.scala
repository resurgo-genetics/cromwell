
import wom.callable.Callable.InputDefinition
import wom.values.WomValue

package object wom {
  type WomEvaluatedCallInputs = Map[InputDefinition, WomValue]
}
