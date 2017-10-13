package wom

import scala.util.Try

trait TsvSerializable {
  def tsvSerialize: Try[String]
}