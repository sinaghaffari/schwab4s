package gg.sina.schwab
package models.trader_api

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.*

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

    implicit val cashInitialBalanceReads: Reads[CashInitialBalance] = Json.reads[CashInitialBalance]
    implicit val cashBalanceReads: Reads[CashBalance] = Json.reads[CashBalance]
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

    implicit val marginInitialBalanceReads: Reads[MarginInitialBalance] = Json.reads[MarginInitialBalance]
    implicit val marginBalanceReads: Reads[MarginBalance] = Json.reads[MarginBalance]
  }
  
  sealed trait AccountType extends EnumEntry with UpperSnakecase
  object AccountType extends Enum[AccountType] with PlayJsonEnum[AccountType] {
    case object Cash extends AccountType
    case object Margin extends AccountType

    val values: IndexedSeq[AccountType] = findValues
  }

  implicit val marginAccountReads: Reads[MarginAccount] = Json.reads[MarginAccount]
  implicit val cashAccountReads: Reads[CashAccount] = Json.reads[CashAccount]
  implicit val securitiesAccountReads: Reads[SecuritiesAccount] = (json: JsValue) => (json \ "type").validate[AccountType] match {
    case JsSuccess(AccountType.Cash, _) => cashAccountReads.reads(json)
    case JsSuccess(AccountType.Margin, _) => marginAccountReads.reads(json)
    case error: JsError => error
  }
}
