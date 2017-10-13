
import org.apache.commons.codec.digest.DigestUtils
import wom.callable.Callable.InputDefinition
import wom.values.{Hashable, SymbolHash, WdlFile, WdlValue}

package object wom {
  type WomEvaluatedCallInputs = Map[InputDefinition, WdlValue]

  type FileHasher = WdlFile => SymbolHash

  implicit class HashableString(val value: String) extends AnyVal with Hashable {
    def md5Sum: String = DigestUtils.md5Hex(value)
    def md5SumShort: String = value.md5Sum.substring(0, 8)
  }
}
