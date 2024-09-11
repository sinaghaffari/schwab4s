package gg.sina.schwab
package models.market_data_api

import java.util.UUID
import zio.json._

case class ErrorResponse(
  id: UUID,
  status: Int,
  title: String,
  detail: String,
  source: ErrorResponse.ErrorSource,
) extends Exception

object ErrorResponse {
  case class ErrorSource(
    pointer: Vector[String],
    parameter: String,
    header: String,
  )
  implicit val errorSourceCodec: JsonCodec[ErrorSource] = DeriveJsonCodec.gen[ErrorSource]
  implicit val codec: JsonCodec[ErrorResponse] =
    DeriveJsonCodec.gen[ErrorResponse]
}
