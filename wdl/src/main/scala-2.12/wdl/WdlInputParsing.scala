package wdl

import lenthall.Checked
import wdl.types.{WdlStringType, WdlType}
import wom.executable.Executable
import wom.executable.Executable.{InputParsingFunction, ParsedInputMap}
import wom.types.{WomStringType, WomType}

import scala.util.Try

private [wdl] object WdlInputParsing {

  implicit class WomTypeToWdlType(val womType: WomType) extends AnyVal {
    def toWdlType: WdlType = {
      womType match {
        case _: WomStringType => WdlStringType
        case _ => ???
      }
    }
  }
  private [wdl] lazy val inputCoercionFunction: InputParsingFunction = inputString => {
    import lenthall.validation.Checked._
    import lenthall.validation.Validation._
    import spray.json._

    Try(inputString.parseJson).toErrorOr.toEither flatMap {
      case JsObject(fields) => fields.map({
        case (key, jsValue) => key -> { womType: WomType => womType.toWdlType.coerceRawValue(jsValue).toErrorOr }
      }).validNelCheck
      case other => s"WDL input file must be a valid Json object. Found a ${other.getClass.getSimpleName}".invalidNelCheck[ParsedInputMap]
    }
  }

  def buildWomExecutable(workflow: WdlWorkflow, inputFile: Option[String]): Checked[Executable] = {
    for {
      womDefinition <- workflow.womDefinition.toEither
      executable <- Executable.withInputs(womDefinition, inputCoercionFunction, inputFile)
    } yield executable
  }
}
