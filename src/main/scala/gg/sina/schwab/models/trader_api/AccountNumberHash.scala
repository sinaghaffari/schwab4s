package gg.sina.schwab
package models.trader_api

import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class AccountNumberHash(accountNumber: String, hashValue: String)

object AccountNumberHash {
  implicit val accountNumberHashDecoder: JsonDecoder[AccountNumberHash] = DeriveJsonDecoder.gen[AccountNumberHash]
}
