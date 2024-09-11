package gg.sina.schwab
package models.trader_api

import gg.sina.schwab.JsonCodecs.*
import org.joda.time.DateTime
import zio.json.ast.{Json, JsonCursor}
import zio.json.{DeriveJsonDecoder, JsonDecoder}

sealed trait AccountsInstrument {
  val assetType: AccountsInstrument.AssetType
  val cusip: String
  val symbol: String
  val description: Option[String]
  val instrumentId: Option[Long]
  val netChange: Option[BigDecimal]
}

object AccountsInstrument {
  case class AccountCashEquivalent(
    assetType: AssetType,
    cusip: String,
    symbol: String,
    description: Option[String],
    instrumentId: Option[Long],
    netChange: Option[BigDecimal],
    `type`: AccountCashEquivalent.Type,
  ) extends AccountsInstrument

  case class AccountEquity(
    assetType: AssetType,
    cusip: String,
    symbol: String,
    description: Option[String],
    instrumentId: Option[Long],
    netChange: Option[BigDecimal],
  ) extends AccountsInstrument
  case class AccountFixedIncome(
    assetType: AssetType,
    cusip: String,
    symbol: String,
    description: Option[String],
    instrumentId: Option[Long],
    netChange: Option[BigDecimal],
    maturityDate: DateTime,
    factor: BigDecimal,
    variableRate: BigDecimal,
  ) extends AccountsInstrument
  case class AccountMutualFund(
    assetType: AssetType,
    cusip: String,
    symbol: String,
    description: Option[String],
    instrumentId: Option[Long],
    netChange: Option[BigDecimal],
  ) extends AccountsInstrument
  case class AccountOption(
    assetType: AssetType,
    cusip: String,
    symbol: String,
    description: Option[String],
    instrumentId: Option[Long],
    netChange: Option[BigDecimal],
    optionDeliverables: Vector[AccountOption.AccountAPIOptionDeliverable],
    putCall: AccountOption.PutCall,
    optionMultiplier: Int,
    `type`: AccountOption.Type,
    underlyingSymbol: String,
  ) extends AccountsInstrument
  case class AccountCurrency(
    assetType: AssetType,
    cusip: String,
    symbol: String,
    description: Option[String],
    instrumentId: Option[Long],
    netChange: Option[BigDecimal],
  ) extends AccountsInstrument
  case class AccountCollectiveInvestment(
    assetType: AssetType,
    cusip: String,
    symbol: String,
    description: Option[String],
    instrumentId: Option[Long],
    netChange: Option[BigDecimal],
    `type`: AccountCollectiveInvestment.Type
  ) extends AccountsInstrument


  object AccountCashEquivalent {
    type Type = "SWEEP_VEHICLE" | "SAVINGS" | "MONEY_MARKET_FUND" | "UNKNOWN"
  }
  object AccountOption {
    type Currency = "USD" | "CAD" | "EUR" | "JPY"
    type PutCall = "PUT" | "CALL" | "UNKNOWN"
    type Type = "VANILLA" | "BINARY" | "BARRIER" | "UNKNOWN"
    case class AccountAPIOptionDeliverable(
      symbol: String,
      deliverableUnits: BigDecimal,
      apiCurrencyType: Currency,
      assetType: AssetType
    )
    implicit val accountAPIOptionDeliverableDecoder: JsonDecoder[AccountAPIOptionDeliverable] = DeriveJsonDecoder.gen[AccountAPIOptionDeliverable]
  }
  object AccountCollectiveInvestment {
    type Type = "UNIT_INVESTMENT_TRUST" | "EXCHANGE_TRADED_FUND" | "CLOSED_END_FUND" | "INDEX" | "UNITS"
  }

  type AssetType = "EQUITY" | "OPTION" | "INDEX" | "MUTUAL_FUND" | "CASH_EQUIVALENT" | "FIXED_INCOME" | "CURRENCY" | "COLLECTIVE_INVESTMENT"

  private implicit val accountCashEquivalentDecoder: JsonDecoder[AccountCashEquivalent] = DeriveJsonDecoder.gen[AccountCashEquivalent]
  private implicit val accountEquityDecoder: JsonDecoder[AccountEquity] = DeriveJsonDecoder.gen[AccountEquity]
  private implicit val accountFixedIncomeDecoder: JsonDecoder[AccountFixedIncome] = DeriveJsonDecoder.gen[AccountFixedIncome]
  private implicit val accountMutualFundDecoder: JsonDecoder[AccountMutualFund] = DeriveJsonDecoder.gen[AccountMutualFund]
  private implicit val accountOptionDecoder: JsonDecoder[AccountOption] = DeriveJsonDecoder.gen[AccountOption]
  private implicit val accountCurrencyDecoder: JsonDecoder[AccountCurrency] = DeriveJsonDecoder.gen[AccountCurrency]
  private implicit val accountCollectiveInvestmentDecoder: JsonDecoder[AccountCollectiveInvestment] = DeriveJsonDecoder.gen[AccountCollectiveInvestment]
  implicit val accountsInstrumentDecoder: JsonDecoder[AccountsInstrument] = JsonDecoder[Json].mapOrFail { json =>
    json.get(JsonCursor.field("assetType").isString) match {
      case Left(value) => Left(value)
      case Right(Json.Str("EQUITY")) => json.as[AccountEquity]
      case Right(Json.Str("OPTION")) => json.as[AccountOption]
      case Right(Json.Str("INDEX")) => json.as[AccountCollectiveInvestment]
      case Right(Json.Str("MUTUAL_FUND")) => json.as[AccountMutualFund]
      case Right(Json.Str("CASH_EQUIVALENT")) => json.as[AccountCashEquivalent]
      case Right(Json.Str("FIXED_INCOME")) => json.as[AccountFixedIncome]
      case Right(Json.Str("CURRENCY")) => json.as[AccountCurrency]
      case Right(Json.Str("COLLECTIVE_INVESTMENT")) => json.as[AccountCollectiveInvestment]
      case Right(Json.Str(other)) => Left(f"Unexpected assetType ($other)")
    }
  }
}
