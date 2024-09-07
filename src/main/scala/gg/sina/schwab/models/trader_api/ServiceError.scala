package gg.sina.schwab
package models.trader_api

import play.api.libs.json.{Json, Reads}

case class ServiceError(message: String, errors: Vector[String]) extends Exception(message)

object ServiceError {
  implicit val serviceErrorReads: Reads[ServiceError] = Json.reads[ServiceError]
}