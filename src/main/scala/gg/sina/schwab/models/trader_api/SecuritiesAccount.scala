package gg.sina.schwab
package models.trader_api

import zio.json.ast.{Json, JsonCursor}
import zio.json.{DeriveJsonDecoder, JsonDecoder}

sealed trait SecuritiesAccount {
  val `type`: SecuritiesAccount.AccountType
  val accountNumber: String
  val roundTrips: Int
  val isDayTrader: Boolean
  val isClosingOnlyRestricted: Boolean
  val pfcbFlag: Boolean
  val positions: Option[Vector[Position]]
  val initialBalances: SecuritiesAccount.InitialBalance
}

object SecuritiesAccount {
  case class MarginAccount(
    `type`: AccountType,
    accountNumber: String,
    roundTrips: Int,
    isDayTrader: Boolean,
    isClosingOnlyRestricted: Boolean,
    pfcbFlag: Boolean,
    positions: Option[Vector[Position]],
    initialBalances: MarginAccount.MarginInitialBalance,
    currentBalances: MarginAccount.MarginBalance,
    projectedBalances: MarginAccount.MarginBalance,
  ) extends SecuritiesAccount
  case class CashAccount(
    `type`: AccountType,
    accountNumber: String,
    roundTrips: Int,
    isDayTrader: Boolean,
    isClosingOnlyRestricted: Boolean,
    pfcbFlag: Boolean,
    positions: Option[Vector[Position]],
    initialBalances: CashAccount.CashInitialBalance,
    currentBalances: CashAccount.CashBalance,
    projectedBalances: CashAccount.CashBalance,
  ) extends SecuritiesAccount

  sealed trait InitialBalance {
    val accountValue: BigDecimal
    val accruedInterest: BigDecimal
    val bondValue: BigDecimal
    val cashAvailableForTrading: BigDecimal
    val cashBalance: BigDecimal
    val cashReceipts: BigDecimal
    val isInCall: Boolean
    val liquidationValue: BigDecimal
    val longOptionMarketValue: BigDecimal
    val longStockValue: BigDecimal
    val moneyMarketFund: BigDecimal
    val mutualFundValue: BigDecimal
    val pendingDeposits: BigDecimal
    val shortOptionMarketValue: BigDecimal
    val shortStockValue: BigDecimal
    val unsettledCash: Option[BigDecimal]
  }
  object CashAccount {
    case class CashInitialBalance(
      accruedInterest: BigDecimal,
      cashAvailableForTrading: BigDecimal,
      cashAvailableForWithdrawal: BigDecimal,
      cashBalance: BigDecimal,
      bondValue: BigDecimal,
      cashReceipts: BigDecimal,
      liquidationValue: BigDecimal,
      longOptionMarketValue: BigDecimal,
      longStockValue: BigDecimal,
      moneyMarketFund: BigDecimal,
      mutualFundValue: BigDecimal,
      shortOptionMarketValue: BigDecimal,
      shortStockValue: BigDecimal,
      isInCall: Boolean,
      unsettledCash: Option[BigDecimal],
      cashDebitCallValue: Option[BigDecimal],
      pendingDeposits: BigDecimal,
      accountValue: BigDecimal,
    ) extends InitialBalance
    case class CashBalance(
      cashAvailableForTrading: BigDecimal,
      cashAvailableForWithdrawal: BigDecimal,
      cashCall: Option[BigDecimal],
      longNonMarginableMarketValue: Option[BigDecimal],
      totalCash: Option[BigDecimal],
      cashDebitCallValue: Option[BigDecimal],
      unsettledCash: Option[BigDecimal],
    )

    implicit val cashInitialBalanceDecoder: JsonDecoder[CashInitialBalance] = DeriveJsonDecoder.gen[CashInitialBalance]
    implicit val cashBalanceDecoder: JsonDecoder[CashBalance] = DeriveJsonDecoder.gen[CashBalance]
  }
  object MarginAccount {
    case class MarginInitialBalance(
      accruedInterest: BigDecimal,
      availableFundsNonMarginableTrade: BigDecimal,
      bondValue: BigDecimal,
      buyingPower: BigDecimal,
      cashBalance: BigDecimal,
      cashAvailableForTrading: BigDecimal,
      cashReceipts: BigDecimal,
      dayTradingBuyingPower: BigDecimal,
      dayTradingBuyingPowerCall: BigDecimal,
      dayTradingEquityCall: BigDecimal,
      equity: BigDecimal,
      equityPercentage: BigDecimal,
      liquidationValue: BigDecimal,
      longMarginValue: BigDecimal,
      longOptionMarketValue: BigDecimal,
      longStockValue: BigDecimal,
      maintenanceCall: BigDecimal,
      maintenanceRequirement: BigDecimal,
      margin: BigDecimal,
      marginEquity: BigDecimal,
      moneyMarketFund: BigDecimal,
      mutualFundValue: BigDecimal,
      regTCall: BigDecimal,
      shortMarginValue: Option[BigDecimal],
      shortOptionMarketValue: BigDecimal,
      shortStockValue: BigDecimal,
      totalCash: BigDecimal,
      isInCall: Boolean,
      unsettledCash: Option[BigDecimal],
      pendingDeposits: BigDecimal,
      marginBalance: Option[BigDecimal],
      shortBalance: Option[BigDecimal],
      accountValue: BigDecimal,
    ) extends InitialBalance
    case class MarginBalance(
      availableFunds: BigDecimal,
      availableFundsNonMarginableTrade: BigDecimal,
      buyingPower: BigDecimal,
      buyingPowerNonMarginableTrade: Option[BigDecimal],
      dayTradingBuyingPower: BigDecimal,
      dayTradingBuyingPowerCall: Option[BigDecimal],
      equity: Option[BigDecimal],
      equityPercentage: Option[BigDecimal],
      longMarginValue: Option[BigDecimal],
      maintenanceCall: BigDecimal,
      maintenanceRequirement: Option[BigDecimal],
      marginBalance: Option[BigDecimal],
      regTCall: BigDecimal,
      shortBalance: Option[BigDecimal],
      shortMarginValue: Option[BigDecimal],
      sma: Option[BigDecimal],
      isInCall: Option[Boolean],
      stockBuyingPower: Option[BigDecimal],
      optionBuyingPower: Option[BigDecimal],
    )

    implicit val marginInitialBalanceDecoder: JsonDecoder[MarginInitialBalance] = DeriveJsonDecoder.gen[MarginInitialBalance]
    implicit val marginBalanceDecoder: JsonDecoder[MarginBalance] = DeriveJsonDecoder.gen[MarginBalance]
  }
  
  type AccountType = "CASH" | "MARGIN"
  
  private implicit val marginAccountDecoder: JsonDecoder[MarginAccount] = DeriveJsonDecoder.gen[MarginAccount]
  private implicit val cashAccountDecoder: JsonDecoder[CashAccount] = DeriveJsonDecoder.gen[CashAccount]
  
  implicit val securitiesAccountDecoder: JsonDecoder[SecuritiesAccount] = JsonDecoder[Json].mapOrFail { json =>
    json.get(JsonCursor.field("type").isString) match {
      case Left(value) => Left(value)
      case Right(Json.Str("CASH")) => json.as[CashAccount]
      case Right(Json.Str("MARGIN")) => json.as[MarginAccount]
      case Right(Json.Str(accountType)) => Left(f"$accountType is not a valid type.")
    }
  }
}
