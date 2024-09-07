package gg.sina.schwab
package models.trader_api

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import gg.sina.schwab.JsonReads.*
import org.joda.time.DateTime
import play.api.libs.json.*

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
    sealed trait Type extends EnumEntry with UpperSnakecase
    object Type extends Enum[Type] with PlayJsonEnum[Type] {
      case object SweepVehicle extends Type
      case object Savings extends Type
      case object MoneyMarketFund extends Type
      case object Unknown extends Type

      val values: IndexedSeq[Type] = findValues
    }
  }
  object AccountOption {
    case class AccountAPIOptionDeliverable(
      symbol: String,
      deliverableUnits: BigDecimal,
      apiCurrencyType: Currency,
      assetType: AssetType
    )

    sealed trait Currency extends EnumEntry with UpperSnakecase
    object Currency extends Enum[Currency] with PlayJsonEnum[Currency] {
      case object USD extends Currency
      case object CAD extends Currency
      case object EUR extends Currency
      case object JPY extends Currency

      val values: IndexedSeq[Currency] = findValues
    }

    sealed trait PutCall extends EnumEntry with UpperSnakecase
    object PutCall extends Enum[PutCall] with PlayJsonEnum[PutCall] {
      case object Put extends PutCall
      case object Call extends PutCall
      case object Unknown extends PutCall

      val values: IndexedSeq[PutCall] = findValues
    }

    sealed trait Type extends EnumEntry with UpperSnakecase
    object Type extends Enum[Type] with PlayJsonEnum[Type] {
      case object Vanilla extends Type
      case object Binary extends Type
      case object Barrier extends Type
      case object Unknown extends Type

      val values: IndexedSeq[Type] = findValues
    }

    implicit val accountAPIOptionDeliverableReads: Reads[AccountAPIOptionDeliverable] = Json.reads[AccountAPIOptionDeliverable]
  }
  object AccountCollectiveInvestment {
    sealed trait Type extends EnumEntry with UpperSnakecase
    object Type extends Enum[Type] with PlayJsonEnum[Type] {
      case object UnitInvestmentTrust extends Type
      case object ExchangeTradedFund extends Type
      case object ClosedEndFund extends Type
      case object Index extends Type
      case object Units extends Type

      val values: IndexedSeq[Type] = findValues
    }
  }

  sealed trait AssetType extends EnumEntry with UpperSnakecase
  object AssetType extends Enum[AssetType] with PlayJsonEnum[AssetType] {
    case object Equity extends AssetType
    case object Option extends AssetType
    case object Index extends AssetType
    case object MutualFund extends AssetType
    case object CashEquivalent extends AssetType
    case object FixedIncome extends AssetType
    case object Currency extends AssetType
    case object CollectiveInvestment extends AssetType

    val values: IndexedSeq[AssetType] = findValues
  }

  implicit val accountCashEquivalentReads: Reads[AccountCashEquivalent] = Json.reads[AccountCashEquivalent]
  implicit val accountEquityReads: Reads[AccountEquity] = Json.reads[AccountEquity]
  implicit val accountFixedIncomeReads: Reads[AccountFixedIncome] = Json.reads[AccountFixedIncome]
  implicit val accountMutualFundReads: Reads[AccountMutualFund] = Json.reads[AccountMutualFund]
  implicit val accountOptionReads: Reads[AccountOption] = Json.reads[AccountOption]
  implicit val accountCurrencyReads: Reads[AccountCurrency] = Json.reads[AccountCurrency]
  implicit val accountCollectiveInvestmentReads: Reads[AccountCollectiveInvestment] = Json.reads[AccountCollectiveInvestment]
  implicit val accountsInstrumentReads: Reads[AccountsInstrument] = (json: JsValue) => (json \ "assetType").validate[AssetType] match {
    case JsSuccess(AssetType.Equity, _) => accountEquityReads.reads(json)
    case JsSuccess(AssetType.Option, _) => accountOptionReads.reads(json)
    case JsSuccess(AssetType.Index, _) => accountCollectiveInvestmentReads.reads(json)
    case JsSuccess(AssetType.MutualFund, _) => accountMutualFundReads.reads(json)
    case JsSuccess(AssetType.CashEquivalent, _) => accountCashEquivalentReads.reads(json)
    case JsSuccess(AssetType.FixedIncome, _) => accountFixedIncomeReads.reads(json)
    case JsSuccess(AssetType.Currency, _) => accountCurrencyReads.reads(json)
    case JsSuccess(AssetType.CollectiveInvestment, _) => accountCollectiveInvestmentReads.reads(json)
    case error: JsError => error
  }
}
