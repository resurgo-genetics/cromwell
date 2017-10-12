package cwl.types

import wom.types.{WomArrayType, WomOptionalType, WomType}

trait CwlType extends WomType {
  /** Make an array type of the receiver type. e.g., if invoked on a CwlStringType this will make a CwlArrayType(CwlStringType). */
  // This seems like it would become a legit thing to do when supporting ScatterFeatureRequirement.
  override def arrayType: WomArrayType = CwlArrayType(this)

  /** Make an optional type of the receiver type. e.g., if invoked on a CwlStringType this will make a CwlOptionalType(CwlStringType) */
  // TODO CWL Not totally sure it's legit for this to get called in the CWL context but supporting it for now.
  override def optionalType: WomOptionalType = CwlOptionalType(this)
}
