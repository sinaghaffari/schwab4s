package gg.sina.schwab
package models.trader_api

import play.api.libs.json.{Json, Reads}

case class AccountNumberHash(accountNumber: String, hashValue: String)

object AccountNumberHash {
  implicit val accountNumberHashReads: Reads[AccountNumberHash] = Json.reads[AccountNumberHash]
}
