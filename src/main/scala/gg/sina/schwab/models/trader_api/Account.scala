package gg.sina.schwab
package models.trader_api

import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class Account(securitiesAccount: SecuritiesAccount)

object Account {
  implicit val accountCodec: JsonDecoder[Account] = DeriveJsonDecoder.gen[Account]
}
