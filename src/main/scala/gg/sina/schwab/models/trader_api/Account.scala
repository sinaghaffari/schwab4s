package gg.sina.schwab
package models.trader_api

import play.api.libs.json.{Json, Reads}

case class Account(securitiesAccount: SecuritiesAccount)

object Account {
  implicit val account: Reads[Account] = Json.reads[Account]
}
