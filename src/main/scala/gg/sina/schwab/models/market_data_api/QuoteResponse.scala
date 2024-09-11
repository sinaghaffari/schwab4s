package gg.sina.schwab.models.market_data_api

import org.joda.time.{DateTime, LocalDate}
import zio.json.ast.{Json, JsonCursor}
import zio.json.{DeriveJsonDecoder, JsonDecoder}
import gg.sina.schwab.JsonCodecs._

sealed trait QuoteResponse

object QuoteResponse {
  sealed trait ValidQuoteResponse[Q <: Quote, R <: Reference] extends QuoteResponse {
    val assetMainType: QuoteResponse.AssetMainType
    val ssid: Option[Long]
    val symbol: String
    val realtime: Boolean
    val quote: Q
    val reference: R
  }

  sealed trait InvalidQuoteResponse extends QuoteResponse

  sealed trait Quote {
    val closePrice: BigDecimal
    val netChange: BigDecimal
    val securityStatus: String
    val totalVolume: Option[Long]
    val tradeTime: DateTime
    val lastPrice: BigDecimal
  }

  sealed trait Reference {
    val description: String
    val exchange: String
    val exchangeName: String
  }

  type AssetMainType = "EQUITY" | "FOREX" | "FUTURE" | "FUTURE_OPTION" | "INDEX" | "MUTUAL_FUND" | "OPTION"
  type FundStrategy = "A" | "L" | "P" | "Q" | "S"

  /**
   * Fundamentals of a security.
   *
   * @param avg10DaysVolume    : Average 10 day volume
   * @param avg1YearVolume     : Average 1 year volume
   * @param declarationDate    : Declaration date in yyyy-mm-ddThh:mm:ssZ
   * @param divAmount          : Dividend Amount
   * @param divExDate          : Dividend date in yyyy-mm-ddThh:mm:ssZ
   * @param divFreq            : Dividend frequency 1 – once a year or annually 2 – 2x a year or semi-annualy 3 - 3x a year (ex. ARCO, EBRPF) 4 – 4x a year or quarterly 6 - 6x per yr or every other month 11 – 11x a year (ex. FBND, FCOR) 12 – 12x a year or monthly
   * @param divPayAmount       : Dividend Pay Amount
   * @param divPayDate         : Dividend pay date in yyyy-mm-ddThh:mm:ssZ
   * @param divYield           : Dividend yield
   * @param eps                : Earnings per Share
   * @param fundLeverageFactor : Fund Leverage Factor + > 0 <-
   * @param fundStrategy       : Fund strategy
   * @param nextDivExDate      : Next Dividend date
   * @param nextDivPayDate     : Next Dividend pay date
   * @param psRatio            : P/E Ratio
   */
  case class Fundamental(
    avg10DaysVolume: BigDecimal,
    avg1YearVolume: BigDecimal,
    declarationDate: Option[DateTime],
    divAmount: BigDecimal,
    divExDate: Option[DateTime],
    divFreq: Option[Int],
    divPayAmount: BigDecimal,
    divPayDate: Option[DateTime],
    divYield: BigDecimal,
    eps: BigDecimal,
    fundLeverageFactor: BigDecimal,
    fundStrategy: Option[FundStrategy],
    nextDivExDate: Option[DateTime],
    nextDivPayDate: Option[DateTime],
    psRatio: Option[BigDecimal],
  )

  /**
   * Quote info of Equity security
   *
   * @param assetMainType Instrument's asset type
   * @param assetSubType  Asset Sub Type (only there if applicable)
   * @param ssid          SSID of instrument
   * @param symbol        Symbol of instrument
   * @param realtime      is quote realtime
   * @param quoteType     NBBO - realtime, NFL - Non-fee liable quote.
   * @param extended      Quote data for extended hours
   * @param fundamental   Fundamentals of a security
   * @param quote         Quote data of Equity security
   * @param reference     Reference data of Equity security
   * @param regular       Market info of security
   */
  case class EquityResponse(
    assetMainType: "EQUITY",
    assetSubType: Option[EquityResponse.EquityAssetSubType],
    ssid: Option[Long],
    symbol: String,
    realtime: Boolean,
    quoteType: Option[EquityResponse.QuoteType],
    extended: Option[EquityResponse.ExtendedMarket],
    fundamental: Option[Fundamental],
    quote: EquityResponse.QuoteEquity,
    reference: EquityResponse.ReferenceEquity,
    regular: Option[EquityResponse.RegularMarket],
  ) extends ValidQuoteResponse[EquityResponse.QuoteEquity, EquityResponse.ReferenceEquity]

  case class OptionResponse(
    assetMainType: "OPTION",
    ssid: Option[Long],
    symbol: String,
    realtime: Boolean,
    quote: OptionResponse.QuoteOption,
    reference: OptionResponse.ReferenceOption
  ) extends ValidQuoteResponse[OptionResponse.QuoteOption, OptionResponse.ReferenceOption]

  case class ForexResponse(
    assetMainType: "FOREX",
    ssid: Option[Long],
    symbol: String,
    realtime: Boolean,
    quote: ForexResponse.QuoteForex,
    reference: ForexResponse.ReferenceForex,
  ) extends ValidQuoteResponse[ForexResponse.QuoteForex, ForexResponse.ReferenceForex]

  case class FutureResponse(
    assetMainType: "FUTURE",
    ssid: Option[Long],
    symbol: String,
    realtime: Boolean,
    quote: FutureResponse.QuoteFuture,
    reference: FutureResponse.ReferenceFuture
  ) extends ValidQuoteResponse[FutureResponse.QuoteFuture, FutureResponse.ReferenceFuture]

  case class FutureOptionResponse(
    assetMainType: "FUTURE_OPTION",
    ssid: Option[Long],
    symbol: String,
    realtime: Boolean,
    quote: FutureOptionResponse.QuoteFutureOption,
    reference: FutureOptionResponse.ReferenceFutureOption,
  ) extends ValidQuoteResponse[FutureOptionResponse.QuoteFutureOption, FutureOptionResponse.ReferenceFutureOption]

  case class IndexResponse(
    assetMainType: "INDEX",
    ssid: Option[Long],
    symbol: String,
    realtime: Boolean,
    quote: IndexResponse.QuoteIndex,
    reference: IndexResponse.ReferenceIndex,
  ) extends ValidQuoteResponse[IndexResponse.QuoteIndex, IndexResponse.ReferenceIndex]

  case class MutualFundResponse(
    assetMainType: "MUTUAL_FUND",
    ssid: Option[Long],
    symbol: String,
    realtime: Boolean,
    fundamental: Option[Fundamental],
    quote: MutualFundResponse.QuoteMutualFund,
    reference: MutualFundResponse.ReferenceMutualFund,
  ) extends ValidQuoteResponse[MutualFundResponse.QuoteMutualFund, MutualFundResponse.ReferenceMutualFund]

  case class QuoteError(
    invalidCusips: Option[Vector[String]],
    invalidSSIDs: Option[Vector[Long]],
    invalidSymbols: Option[Vector[String]],
  ) extends InvalidQuoteResponse

  object EquityResponse {
    type EquityAssetSubType = "COE" | "PRF" | "ADR" | "GDR" | "CEF" | "ETF" | "ETN" | "UIT" | "WAR" | "RGT"
    type QuoteType = "NBBO" | "NFL"

    /**
     * Quote data for extended hours
     *
     * @param askPrice    Extended market ask price
     * @param askSize     Extended market ask size
     * @param bidPrice    Extended market bid price
     * @param lastPrice   Extended market last price
     * @param lastSize    Regular market last size
     * @param mark        mark price
     * @param quoteTime   Extended market quote time
     * @param totalVolume Total volume
     * @param tradeTime   Extended market trade time
     */
    case class ExtendedMarket(
      askPrice: BigDecimal,
      askSize: Int,
      bidPrice: BigDecimal,
      lastPrice: BigDecimal,
      lastSize: Int,
      mark: BigDecimal,
      quoteTime: DateTime,
      totalVolume: Long,
      tradeTime: DateTime,
    )

    /**
     * Quote data of Equity security
     *
     * @param `52WeekHigh`      Higest price traded in the past 12 months, or 52 weeks
     * @param `52WeekLow`       Lowest price traded in the past 12 months, or 52 weeks
     * @param askMICId          ask MIC code
     * @param askPrice          Current Best Ask Price
     * @param askSize           Number of shares for ask
     * @param askTime           Last ask time in milliseconds since Epoch
     * @param bidMICId          bid MIC code
     * @param bidPrice          Current Best Bid Price
     * @param bidSize           Number of shares for bid
     * @param bidTime           Last bid time in milliseconds since Epoch
     * @param closePrice        Previous day's closing price
     * @param highPrice         Day's high trade price
     * @param lastMICId         Last MIC Code
     * @param lastPrice         Last price
     * @param lastSize          Number of shares traded with last trade
     * @param lowPrice          Day's low trade price
     * @param mark              Mark price
     * @param markChange        Mark Price change
     * @param markPercentChange Mark Price percent change
     * @param netChange         Current Last-Prev Close
     * @param netPercentChange  Net Percentage Change
     * @param openPrice         Price at market open
     * @param quoteTime         Last quote time
     * @param securityStatus    Status of security
     * @param totalVolume       Aggregated shares traded throughout the day, including pre/post market hours.
     * @param tradeTime         Last trade time
     * @param volatility        Option Risk/Volatility Measurement
     */
    case class QuoteEquity(
      `52WeekHigh`: BigDecimal,
      `52WeekLow`: BigDecimal,
      askMICId: Option[String],
      askPrice: BigDecimal,
      askSize: Int,
      askTime: DateTime,
      bidMICId: Option[String],
      bidPrice: BigDecimal,
      bidSize: Int,
      bidTime: DateTime,
      closePrice: BigDecimal,
      highPrice: BigDecimal,
      lastMICId: Option[String],
      lastPrice: BigDecimal,
      lastSize: Int,
      lowPrice: BigDecimal,
      mark: BigDecimal,
      markChange: BigDecimal,
      markPercentChange: BigDecimal,
      netChange: BigDecimal,
      netPercentChange: BigDecimal,
      openPrice: BigDecimal,
      quoteTime: DateTime,
      securityStatus: String,
      totalVolume: Option[Long],
      tradeTime: DateTime,
      volatility: Option[BigDecimal],
    ) extends Quote

    /**
     * Reference data of Equity security
     *
     * @param cusip          CUSIP of Instrument
     * @param description    Description of Instrument
     * @param exchange       Exchange Code
     * @param exchangeName   Exchange Name
     * @param fsiDesc        FSI Desc
     * @param htbQuantity    Hard to borrow quantity.
     * @param htbRate        Hard to borrow rate.
     * @param isHardToBorrow is Hard to borrow security.
     * @param isShortable    is shortable security.
     * @param otcMarketTier  OTC Market Tier
     */
    case class ReferenceEquity(
      cusip: String,
      description: String,
      exchange: String,
      exchangeName: String,
      fsiDesc: Option[String],
      htbQuantity: Option[Int],
      htbRate: Option[BigDecimal],
      isHardToBorrow: Option[Boolean],
      isShortable: Option[Boolean],
      otcMarketTier: Option[String],
    ) extends Reference

    /**
     * Market info of security
     *
     * @param regularMarketLastPrice     Regular market last price
     * @param regularMarketLastSize      Regular market last size
     * @param regularMarketNetChange     Regular market net change
     * @param regularMarketPercentChange Regular market percent change
     * @param regularMarketTradeTime     Regular market trade time in milliseconds since Epoch
     */
    case class RegularMarket(
      regularMarketLastPrice: BigDecimal,
      regularMarketLastSize: Int,
      regularMarketNetChange: BigDecimal,
      regularMarketPercentChange: BigDecimal,
      regularMarketTradeTime: DateTime,
    )
    private[QuoteResponse] implicit val extendedMarketDecoder: JsonDecoder[ExtendedMarket] = DeriveJsonDecoder.gen[ExtendedMarket]
    private[QuoteResponse] implicit val quoteEquityDecoder: JsonDecoder[QuoteEquity] = DeriveJsonDecoder.gen[QuoteEquity]
    private[QuoteResponse] implicit val referenceEquityDecoder: JsonDecoder[ReferenceEquity] = DeriveJsonDecoder.gen[ReferenceEquity]
    private[QuoteResponse] implicit val regularMarketDecoder: JsonDecoder[RegularMarket] = DeriveJsonDecoder.gen[RegularMarket]
  }
  object OptionResponse {
    type ContractType = "C" | "P"
    type ExerciseType = "A" | "E"
    type ExpirationType = "S"| "W" | "M" | "Q"
    type SettlementType = "AM" | "PM" | "P"

    /**
     * Quote data of Option security
     *
     * @param `52WeekHigh`           Highest price traded in the past 12 months, or 52 weeks
     * @param `52WeekLow`            Lowest price traded in the past 12 months, or 52 weeks
     * @param askPrice               Current Best Ask Price
     * @param askSize                Number of shares for ask
     * @param bidPrice               Current Best Bid Price
     * @param bidSize                Number of shares for bid
     * @param closePrice             Previous day's closing price
     * @param delta                  Delta Value
     * @param gamma                  Gamma Value
     * @param highPrice              Day's high trade price
     * @param indAskPrice            Indicative Ask Price applicable only for Indicative Option Symbols
     * @param indBidPrice            Indicative Bid Price applicable only for Indicative Option Symbols
     * @param indQuoteTime           Indicative Quote Time applicable only for Indicative Option Symbols
     * @param impliedYield           Implied Yield
     * @param lastPrice              Last Price
     * @param lastSize               Number of shares traded with last trade
     * @param lowPrice               Day's low trade price
     * @param mark                   Mark price
     * @param markChange             Mark Price change
     * @param markPercentChange      Mark Price percent change
     * @param moneyIntrinsicValue    Money Intrinsic Value
     * @param netChange              Current Last-Prev Close
     * @param netPercentChange       Net Percentage Change
     * @param openInterest           Open Interest
     * @param openPrice              Price at market open
     * @param quoteTime              Last quote time in milliseconds since Epoch
     * @param rho                    Rho Value
     * @param securityStatus         Status of security
     * @param theoreticalOptionValue Theoretical option Value
     * @param theta                  Theta Value
     * @param timeValue              Time Value
     * @param totalVolume            Aggregated shares traded throughout the day, including pre/post market hours.
     * @param tradeTime              Last trade time in milliseconds since Epoch
     * @param underlyingPrice        Underlying Price
     * @param vega                   Vega Value
     * @param volatility             Option Risk/Volatility Measurement
     */
    case class QuoteOption(
      `52WeekHigh`: BigDecimal,
      `52WeekLow`: BigDecimal,
      askPrice: BigDecimal,
      askSize: Int,
      bidPrice: BigDecimal,
      bidSize: Int,
      closePrice: BigDecimal,
      delta: BigDecimal,
      gamma: BigDecimal,
      highPrice: BigDecimal,
      indAskPrice: BigDecimal,
      indBidPrice: BigDecimal,
      indQuoteTime: DateTime,
      impliedYield: BigDecimal,
      lastPrice: BigDecimal,
      lastSize: Int,
      lowPrice: BigDecimal,
      mark: BigDecimal,
      markChange: BigDecimal,
      markPercentChange: BigDecimal,
      moneyIntrinsicValue: BigDecimal,
      netChange: BigDecimal,
      netPercentChange: BigDecimal,
      openInterest: BigDecimal,
      openPrice: BigDecimal,
      quoteTime: DateTime,
      rho: BigDecimal,
      securityStatus: String,
      theoreticalOptionValue: BigDecimal,
      theta: BigDecimal,
      timeValue: BigDecimal,
      totalVolume: Option[Long],
      tradeTime: DateTime,
      underlyingPrice: BigDecimal,
      vega: BigDecimal,
      volatility: BigDecimal,
    ) extends Quote

    /**
     *
     * Reference data of Option security
     *
     * @param contractType     Indicates call or put
     * @param cusip            CUSIP of Instrument
     * @param daysToExpiration Days to Expiration
     * @param deliverables     Unit of trade
     * @param description      Description of Instrument
     * @param exchange         Exchange Code
     * @param exchangeName     Exchange Name
     * @param exerciseType     option contract exercise type America or European
     * @param expirationDay    Expiration Day
     * @param expirationMonth  Expiration Month
     * @param expirationType   M for End Of Month Expiration Calendar Cycle. (To match the last business day of the month), Q for Quarterly expirations (last business day of the quarter month MAR/JUN/SEP/DEC), W for Weekly expiration (also called Friday Short Term Expirations) and S for Expires 3rd Friday of the month (also known as regular options).
     * @param expirationYear   Expiration Year
     * @param isPennyPilot     Is this contract part of the Penny Pilot program
     * @param lastTradingDay   Last trading day
     * @param multiplier       Option multiplier
     * @param settlementType   option contract settlement type AM or PM
     * @param strikePrice      Strike Price
     * @param underlying       A company, index or fund name
     */
    case class ReferenceOption(
      contractType: ContractType,
      cusip: Option[String],
      daysToExpiration: Int,
      deliverables: String,
      description: String,
      exchange: String,
      exchangeName: String,
      exerciseType: ExerciseType,
      expirationDay: Int,
      expirationMonth: Int,
      expirationType: ExpirationType,
      expirationYear: Int,
      isPennyPilot: Boolean,
      lastTradingDay: LocalDate,
      multiplier: BigDecimal,
      settlementType: SettlementType,
      strikePrice: BigDecimal,
      underlying: String,
    ) extends Reference

    private[QuoteResponse] implicit val quoteOptionDecoder: JsonDecoder[QuoteOption] = DeriveJsonDecoder.gen[QuoteOption]
    private[QuoteResponse] implicit val referenceOptionDecoder: JsonDecoder[ReferenceOption] = DeriveJsonDecoder.gen[ReferenceOption]

  }
  object ForexResponse {
    case class QuoteForex(
      `52WeekHigh`: BigDecimal,
      `52WeekLow`: BigDecimal,
      askPrice: BigDecimal,
      askSize: Int,
      bidPrice: BigDecimal,
      bidSize: Int,
      closePrice: BigDecimal,
      highPrice: BigDecimal,
      lastPrice: BigDecimal,
      lastSize: Int,
      lowPrice: BigDecimal,
      mark: BigDecimal,
      netChange: BigDecimal,
      netPercentChange: BigDecimal,
      openPrice: BigDecimal,
      quoteTime: DateTime,
      securityStatus: String,
      tick: BigDecimal,
      tickAmount: BigDecimal,
      totalVolume: Option[Long],
      tradeTime: DateTime
    ) extends Quote

    case class ReferenceForex(
      description: String,
      exchange: String,
      exchangeName: String,
      isTradable: Boolean,
      marketMaker: Option[String],
      product: Option[String],
      tradingHours: Option[String],
    ) extends Reference

    private[QuoteResponse] implicit val quoteForexDecoder: JsonDecoder[QuoteForex] = DeriveJsonDecoder.gen[QuoteForex]
    private[QuoteResponse] implicit val referenceForexDecoder: JsonDecoder[ReferenceForex] = DeriveJsonDecoder.gen[ReferenceForex]
  }
  object FutureResponse {
    case class QuoteFuture(
      askMICId: Option[String],
      askPrice: BigDecimal,
      askSize: Int,
      askTime: Long,
      bidMICId: Option[String],
      bidPrice: BigDecimal,
      bidSize: Int,
      bidTime: DateTime,
      closePrice: BigDecimal,
      futurePercentChange: BigDecimal,
      highPrice: BigDecimal,
      lastMICId: Option[String],
      lastPrice: BigDecimal,
      lastSize: Int,
      lowPrice: BigDecimal,
      mark: BigDecimal,
      netChange: BigDecimal,
      openInterest: Int,
      openPrice: BigDecimal,
      quoteTime: DateTime,
      quotedInSession: Boolean,
      securityStatus: String,
      settleTime: DateTime,
      tick: BigDecimal,
      tickAmount: BigDecimal,
      totalVolume: Option[Long],
      tradeTime: DateTime,
    ) extends Quote

    case class ReferenceFuture(
      description: String,
      exchange: String,
      exchangeName: String,
      futureActiveSymbol: Option[String],
      futureExpirationDate: LocalDate,
      futureIsActive: Boolean,
      futureMultiplier: BigDecimal,
      futurePriceFormat: String,
      futureSettlementPrice: BigDecimal,
      futureTradingHours: String,
      product: String,
    ) extends Reference

    private[QuoteResponse] implicit val quoteFutureDecoder: JsonDecoder[QuoteFuture] = DeriveJsonDecoder.gen[QuoteFuture]
    private[QuoteResponse] implicit val referenceFutureDecoder: JsonDecoder[ReferenceFuture] = DeriveJsonDecoder.gen[ReferenceFuture]
  }
  object FutureOptionResponse {
    case class QuoteFutureOption(
      askMICId: Option[String],
      askPrice: BigDecimal,
      askSize: Int,
      bidMICId: Option[String],
      bidPrice: BigDecimal,
      bidSize: Int,
      closePrice: BigDecimal,
      highPrice: BigDecimal,
      lastMICId: String,
      lastPrice: BigDecimal,
      lastSize: Int,
      lowPrice: BigDecimal,
      mark: BigDecimal,
      markChange: BigDecimal,
      netChange: BigDecimal,
      netPercentChange: BigDecimal,
      openInterest: Int,
      openPrice: BigDecimal,
      quoteTime: DateTime,
      securityStatus: String,
      settlementPrice: BigDecimal,
      tick: BigDecimal,
      tickAmount: BigDecimal,
      totalVolume: Option[Long],
      tradeTime: DateTime,
    ) extends Quote

    case class ReferenceFutureOption(
      contractType: OptionResponse.ContractType,
      description: String,
      exchange: String,
      exchangeName: String,
      multiplier: BigDecimal,
      expirationDate: Long,
      expirationStyle: String,
      strikePrice: BigDecimal,
      underlying: String,
    ) extends Reference

    private[QuoteResponse] implicit val quoteFutureOptionDecoder: JsonDecoder[QuoteFutureOption] = DeriveJsonDecoder.gen[QuoteFutureOption]
    private[QuoteResponse] implicit val referenceFutureOptionDecoder: JsonDecoder[ReferenceFutureOption] = DeriveJsonDecoder.gen[ReferenceFutureOption]
  }
  object IndexResponse {
    case class QuoteIndex(
      `52WeekHigh`: BigDecimal,
      `52WeekLow`: BigDecimal,
      closePrice: BigDecimal,
      highPrice: BigDecimal,
      lastPrice: BigDecimal,
      lowPrice: BigDecimal,
      netChange: BigDecimal,
      netPercentChange: BigDecimal,
      openPrice: BigDecimal,
      securityStatus: String,
      totalVolume: Option[Long],
      tradeTime: DateTime,
    ) extends Quote

    case class ReferenceIndex(
      description: String,
      exchange: String,
      exchangeName: String,
    ) extends Reference
    private[QuoteResponse] implicit val quoteIndexDecoder: JsonDecoder[QuoteIndex] = DeriveJsonDecoder.gen[QuoteIndex]
    private[QuoteResponse] implicit val referenceIndexDecoder: JsonDecoder[ReferenceIndex] = DeriveJsonDecoder.gen[ReferenceIndex]

  }
  object MutualFundResponse {
    case class QuoteMutualFund(
      `52WeekHigh`: BigDecimal,
      `52WeekLow`: BigDecimal,
      closePrice: BigDecimal,
      nAV: BigDecimal,
      netChange: BigDecimal,
      netPercentChange: BigDecimal,
      securityStatus: String,
      totalVolume: Option[Long],
      tradeTime: DateTime,
      lastPrice: BigDecimal,
    ) extends Quote

    case class ReferenceMutualFund(
      cusip: String,
      description: String,
      exchange: String,
      exchangeName: String,
    ) extends Reference

    private[QuoteResponse] implicit val quoteMutualFundDecoder: JsonDecoder[QuoteMutualFund] = DeriveJsonDecoder.gen[QuoteMutualFund]
    private[QuoteResponse] implicit val referenceMutualFundDecoder: JsonDecoder[ReferenceMutualFund] = DeriveJsonDecoder.gen[ReferenceMutualFund]
  }
  private implicit val fundamentalDecoder: JsonDecoder[Fundamental] = DeriveJsonDecoder.gen[Fundamental]

  private implicit val equityResponseDecoder: JsonDecoder[EquityResponse] =
    DeriveJsonDecoder.gen[EquityResponse]
  private implicit val optionResponseDecoder: JsonDecoder[OptionResponse] =
    DeriveJsonDecoder.gen[OptionResponse]
  private implicit val forexResponseDecoder: JsonDecoder[ForexResponse] =
    DeriveJsonDecoder.gen[ForexResponse]
  private implicit val futureResponseDecoder: JsonDecoder[FutureResponse] =
    DeriveJsonDecoder.gen[FutureResponse]
  private implicit val futureOptionResponseDecoder: JsonDecoder[FutureOptionResponse] =
    DeriveJsonDecoder.gen[FutureOptionResponse]
  private implicit val indexResponseDecoder: JsonDecoder[IndexResponse] =
    DeriveJsonDecoder.gen[IndexResponse]
  private implicit val mutualFundResponseDecoder: JsonDecoder[MutualFundResponse] =
    DeriveJsonDecoder.gen[MutualFundResponse]
  private implicit val quoteErrorDecoder: JsonDecoder[QuoteError] = DeriveJsonDecoder.gen[QuoteError]


  implicit val quoteResponseDecoder: JsonDecoder[QuoteResponse] = JsonDecoder[Json].mapOrFail { json =>
    json.get(JsonCursor.field("assetMainType").isString) match {
      case Left(value) => json.as[QuoteError]
      case Right(Json.Str("EQUITY")) => json.as[EquityResponse]
      case Right(Json.Str("OPTION")) => json.as[OptionResponse]
      case Right(Json.Str("FOREX")) => json.as[ForexResponse]
      case Right(Json.Str("FUTURE")) => json.as[FutureResponse]
      case Right(Json.Str("FUTURE_OPTION")) => json.as[FutureOptionResponse]
      case Right(Json.Str("INDEX")) => json.as[IndexResponse]
      case Right(Json.Str("MUTUAL_FUND")) => json.as[MutualFundResponse]
      case Right(zio.json.ast.Json.Str(assetMainType)) => Left(f"$assetMainType is an unexpected AssetMainType.")
    }
  }

}
