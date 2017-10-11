
import wom.callable.Callable.InputDefinition
import wom.values.WomValue

package object wom {
  type WomEvaluatedCallInputs = Map[InputDefinition, WomValue]
  type WorkflowSource = String
  type WorkflowJson = String
  type ExecutableInputMap = Map[String, Any]
  type WorkflowCoercedInputs = Map[FullyQualifiedName, WomValue]
  type FullyQualifiedName = String
  type LocallyQualifiedName = String
  // type EvaluatedTaskInputs = Map[Declaration, WomValue]
  type ImportResolver = String => WorkflowSource
}
