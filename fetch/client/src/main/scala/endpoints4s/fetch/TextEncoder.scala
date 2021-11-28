package endpoints4s.fetch

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.typedarray.Uint8Array

//TODO could be used for chunked requests
@js.native
@JSGlobal
class TextEncoder(utfLabel: js.UndefOr[String] = js.undefined) extends js.Object {
  def encode(str: String): Uint8Array = js.native
}
