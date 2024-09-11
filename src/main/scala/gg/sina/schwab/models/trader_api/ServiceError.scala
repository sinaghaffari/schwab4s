package gg.sina.schwab
package models.trader_api

import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class ServiceError(message: String, errors: Vector[String]) extends Exception(message)

object ServiceError {
  implicit val serviceErrorDecoder: JsonDecoder[ServiceError] = DeriveJsonDecoder.gen[ServiceError]
}