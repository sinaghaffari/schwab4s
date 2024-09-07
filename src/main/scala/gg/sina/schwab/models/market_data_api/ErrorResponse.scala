package gg.sina.schwab
package models.market_data_api

import play.api.libs.json.{Json, OFormat}

import java.util.UUID

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
  implicit val errorSourceFormat: OFormat[ErrorSource] = Json.format[ErrorSource]
  implicit val formats: OFormat[ErrorResponse] = Json.format[ErrorResponse]
}
