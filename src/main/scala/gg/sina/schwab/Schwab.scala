package gg.sina.schwab

import gg.sina.schwab.models.market_data_api.QuoteResponse
import gg.sina.schwab.models.trader_api.{Account, AccountNumberHash}
import zio.*
import zio.connect.file.*
import zio.http.*
import zio.json.*

class Schwab(
  private val authRef: Ref.Synchronized[SchwabAuth],
) {
  object MarketDataAPI {
    def quote(symbols: Iterable[String]): RIO[Scope, Map[String, QuoteResponse]] = handle[Map[String, QuoteResponse]](
      Request
        .get("https://api.schwabapi.com/marketdata/v1/quotes")
        .addQueryParam("symbols", symbols.mkString(","))
        .addQueryParam("indicative", "true")
        .addHeader(Header.Accept(MediaType.application.json))
    )
  }
  object TraderAPI {
    object Accounts {
      def accountNumbers: RIO[Scope, Vector[AccountNumberHash]] = handle[Vector[AccountNumberHash]](
        Request
          .get("https://api.schwabapi.com/trader/v1/accounts/accountNumbers")
          .addHeader(Header.Accept(MediaType.application.json))
      )

      def accounts: RIO[Scope, Vector[Account]] = handle[Vector[Account]](
        Request
          .get("https://api.schwabapi.com/trader/v1/accounts/accountNumbers")
          .addQueryParam("fields", "positions")
          .addHeader(Header.Accept(MediaType.application.json))
      )

      def accounts(accountHash: String): RIO[Scope, Account] = handle[Account](
        Request
          .get(f"https://api.schwabapi.com/trader/v1/accounts/accountNumbers/$accountHash")
          .addQueryParam("fields", "positions")
          .addHeader(Header.Accept(MediaType.application.json))
      )
    }
  }
  private def handle[A](request: Request)(implicit decoder: JsonDecoder[A]) = for {
    response <- authRef.modifyZIO(_.request(request))
    body <- response.body.asString
    result <- ZIO
      .fromEither(body.fromJson[A])
      .mapError(error => Exception(f"Response Body: $body\nError: $error"))
  } yield result
}

object Schwab {
  val layer: ZLayer[Scope & FileConnector & Client, Throwable, Schwab] = ZLayer {
    for {
      auth: SchwabAuth <- ZIO.service[SchwabAuth]
        .provideLayer(SchwabAuth.layer)
      authRef <- Ref.Synchronized.make(auth)
    } yield Schwab(authRef)
  }
}

