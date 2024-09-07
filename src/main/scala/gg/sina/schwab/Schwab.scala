package gg.sina.schwab

import gg.sina.agent.Agent
import gg.sina.monadic_simplifier.Simplifiers.*
import gg.sina.monadic_simplifier.json.PlayJsonSimplifiers.*
import gg.sina.schwab.models.market_data_api.QuoteResponse
import gg.sina.schwab.models.trader_api.{Account, AccountNumberHash}
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.util.Timeout
import play.api.libs.json.{JsResult, JsValue, Json, Reads}
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import play.api.libs.ws.{StandaloneWSRequest, StandaloneWSResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class Schwab()(implicit val ws: StandaloneAhcWSClient, ec: ExecutionContext, system: ActorSystem[?], timeout: Timeout) {
  private val authAgent: Agent[SchwabAuth] = Agent(SchwabAuth())

  object MarketDataAPI {
    def quote(symbols: Iterable[String]): Future[Map[String, QuoteResponse]] = quoteReq(symbols).flatMap(getWithRetry)

    private def quoteReq(symbols: Iterable[String]): Future[StandaloneWSRequest] = for {
      agent <- authAgent()
      accessToken = agent.accessToken
    } yield ws.url("https://api.schwabapi.com/marketdata/v1/quotes")
      .withQueryStringParameters(
        "symbols" -> symbols.mkString(","),
        "indicative" -> "true",
      )
      .withHttpHeaders(
        "Authorization" -> f"Bearer $accessToken",
        "Accept" -> "application/json",
      )
  }

  object TraderAPI {
    object Accounts {
      def accountNumbers: Future[Vector[AccountNumberHash]] = accountNumbersReq.flatMap(getWithRetry)

      def accounts: Future[Vector[Account]] = accountsReq.flatMap { req =>
        getWithRetry(req)
      }

      def accounts(accountNumber: String): Future[Account] = singleAccountReq(accountNumber).flatMap(getWithRetry)

      private def accountNumbersReq: Future[StandaloneWSRequest] = for {
        agent <- authAgent().?|
        accessToken = agent.accessToken
      } yield ws.url("https://api.schwabapi.com/trader/v1/accounts/accountNumbers")
        .withHttpHeaders(
          "Authorization" -> f"Bearer $accessToken",
          "Accept" -> "application/json",
        )

      private def accountsReq: Future[StandaloneWSRequest] = for {
        agent <- authAgent().?|
        accessToken = agent.accessToken
      } yield ws.url("https://api.schwabapi.com/trader/v1/accounts")
        .withQueryStringParameters("fields" -> "positions")
        .withHttpHeaders(
          "Authorization" -> f"Bearer $accessToken",
          "Accept" -> "application/json",
        )

      private def singleAccountReq(accountNumber: String): Future[StandaloneWSRequest] = for {
        agent <- authAgent().?|
        accessToken = agent.accessToken
      } yield ws.url(f"https://api.schwabapi.com/trader/v1/accounts/$accountNumber")
        .withQueryStringParameters("fields" -> "positions")
        .withHttpHeaders(
          "Authorization" -> f"Bearer $accessToken",
          "Accept" -> "application/json",
        )
    }
  }

  private def get[A](request: StandaloneWSRequest, handler: StandaloneWSResponse => JsResult[A]): Future[A] = for {
    response <- request
      .get()
    _ <- (response.status == 200) ?| Schwab.SchwabAPIException(response)
    handled <- handler(response).?|
  } yield handled

  private def getWithRetry[A](request: StandaloneWSRequest, handler: StandaloneWSResponse => JsResult[A]): Future[A] = get(request, handler)
    .recoverWith {
      case err: Schwab.SchwabAPIException if err.response.status == 401 =>
        authAgent
          .flatMap(_.refresh)
          .flatMap(_ => get(request, handler))
    }

  private def getWithRetry[A](request: StandaloneWSRequest)(implicit reads: Reads[A]): Future[A] = getWithRetry(request, defaultHandler[A])

  private def defaultHandler[A](response: StandaloneWSResponse)(implicit reads: Reads[A]): JsResult[A] = response
    .body[JsValue]
    .validate[A]
}

object Schwab {
  case class SchwabAPIException(response: StandaloneWSResponse) extends Exception(
    s"""Schwab API Returned an Erroneous Status Code
       |  Status: ${response.status} ${response.statusText}
       |  Body:
       |${SchwabAPIException.parseBody(response)}
       |""".stripMargin
  )
  object SchwabAPIException {
    private def parseBody(response: StandaloneWSResponse): String = Try(Json.parse(response.body))
      .map(Json.prettyPrint)
      .getOrElse(response.body)
      .indent(4)
  }
}
