package gg.sina.schwab
package models.trader_api

import play.api.libs.json.{Json, Reads}

case class Position(
  shortQuantity: BigDecimal,
  averagePrice: BigDecimal,
  currentDayProfitLoss: BigDecimal,
  currentDayProfitLossPercentage: BigDecimal,
  longQuantity: BigDecimal,
  settledLongQuantity: BigDecimal,
  settledShortQuantity: BigDecimal,
  agedQuantity: Option[BigDecimal],
  instrument: AccountsInstrument,
  marketValue: BigDecimal,
  maintenanceRequirement: BigDecimal,
  averageLongPrice: BigDecimal,
  averageShortPrice: Option[BigDecimal],
  taxLotAverageLongPrice: BigDecimal,
  taxLotAverageShortPrice: Option[BigDecimal],
  longOpenProfitLoss: BigDecimal,
  shortOpenProfitLoss: Option[BigDecimal],
  previousSessionLongQuantity: BigDecimal,
  previousSessionShortQuantity: Option[BigDecimal],
  currentDayCost: BigDecimal,
)

object Position {
  implicit val positionReads: Reads[Position] = Json.reads[Position]
}