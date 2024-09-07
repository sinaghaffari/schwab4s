package gg.sina.schwab

import com.typesafe.config.ConfigFactory
import gg.sina.monadic_simplifier.Simplifiers.*
import gg.sina.monadic_simplifier.json.PlayJsonSimplifiers.*
import gg.sina.schwab.SchwabAuth.*
import gg.sina.schwab.models.market_data_api.ErrorResponse
import play.api.libs.json.*
import play.api.libs.ws.DefaultBodyWritables.*
import play.api.libs.ws.JsonBodyReadables.*
import play.api.libs.ws.ahc.{StandaloneAhcWSClient, StandaloneAhcWSRequest}
import play.api.libs.ws.{BodyWritable, StandaloneWSResponse}

import java.net.{URLDecoder, URLEncoder}
import java.nio.file.{Files, Paths}
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.io.StdIn.readLine
import scala.util.Using
import scala.util.chaining.scalaUtilChainingOps

case class SchwabAuth private(
  private[schwab] val accessToken: String,
  private val refreshToken: String,
)(implicit val ws: StandaloneAhcWSClient, ec: ExecutionContext) {

  println(f"Saved new refresh token to file: ${Files.write(Paths.get(SchwabAuth.refreshTokenFile), refreshToken.getBytes("UTF-8"))}")

  def refresh: Future[SchwabAuth] = SchwabAuth
    .refresh(refreshToken)
    .map(authToken => copy(accessToken = authToken.access_token, refreshToken = authToken.refresh_token))

  def get(request: StandaloneAhcWSRequest): Future[StandaloneWSResponse] = {
    request
      .withHttpHeaders(
        "Authorization" -> f"Bearer $accessToken"
      ).get()
      .map {
        case res if res.status != 200 =>
          res
            .body[JsValue]
            .validate[ErrorResponse] match {
            case JsSuccess(value, _) => throw value
            case error: JsError => throw JsResult.Exception(error)
          }
        case res => res
      }
      .recoverWith {
        case err: ErrorResponse if err.status == 401 =>
          println(err)
          refresh.flatMap(_.get(request))
      }
  }

  def post[T: BodyWritable](request: StandaloneAhcWSRequest, body: T): Future[StandaloneWSResponse] = {
    request
      .withHttpHeaders(
        "Authorization" -> f"Bearer $accessToken"
      )
      .post(body)
  }
}

object SchwabAuth {
  private val config = ConfigFactory.load()
  private val appKey: String = config.getString("schwab.app_key")
  private val secret: String = config.getString("schwab.secret")
  private val refreshTokenFile: String = config.getString("schwab.refresh_token_file")
  private val redirectUri: String = "https://127.0.0.1"
  private val encodedCredentials: String = Base64.getEncoder.encodeToString(f"$appKey:$secret".getBytes("UTF-8"))
  private val clientAuthUrl: String = "https://api.schwabapi.com/v1/oauth/authorize?" + Map(
    "client_id" -> appKey,
    "redirect_uri" -> redirectUri
  )
    .map { case (k, v) => f"${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }
    .mkString("&")

  def apply()(implicit ws: StandaloneAhcWSClient, ec: ExecutionContext): Future[SchwabAuth] = auth().recoverWith(_ => initialAuth())

  def initialAuth()(implicit ws: StandaloneAhcWSClient, ec: ExecutionContext): Future[SchwabAuth] = {
    println(f"To Log Into Schwab Click Here: $clientAuthUrl")
    val authCode: String = URLDecoder.decode(readLine("Paste authorization code: "), "UTF-8")
    ws
      .url("https://api.schwabapi.com/v1/oauth/token")
      .withHttpHeaders(
        "Authorization" -> f"Basic $encodedCredentials",
        "Content-Type" -> "application/x-www-form-urlencoded",
      )
      .post(
        Map(
          "grant_type" -> Vector("authorization_code"),
          "code" -> Vector(authCode),
          "redirect_uri" -> Vector(redirectUri),
        )
      )
      .map(_.body[JsValue])
      .map(_.validate[SchwabAuth.AuthTokenResponse])
      .map {
        case JsSuccess(authTokenResponse, _) =>
          SchwabAuth(authTokenResponse.access_token, authTokenResponse.refresh_token)
        case error: JsError =>
          throw JsResult.Exception(error)
      }
  }

  def auth()(implicit ws: StandaloneAhcWSClient, ec: ExecutionContext): Future[SchwabAuth] = for {
    _ <- Future.successful(println("Authing")).?|
    _ = println("Reading refreshToken from file")
    refreshToken <- Using(Source.fromFile(refreshTokenFile))(_.getLines().next())
      .tap(_.failed.foreach(_.printStackTrace()))
      .?|
    _ = println("Refreshing access token")
    authTokenResponse <- refresh(refreshToken).?|
    _ = println("Creating SchwabAuth")
  } yield SchwabAuth(authTokenResponse.access_token, authTokenResponse.refresh_token)

  private def refresh(refreshToken: String)(implicit ws: StandaloneAhcWSClient, ec: ExecutionContext): Future[AuthTokenResponse] = for {
    response <- ws
      .url("https://api.schwabapi.com/v1/oauth/token")
      .withHttpHeaders(
        "Authorization" -> f"Basic $encodedCredentials",
        "Content-Type" -> "application/x-www-form-urlencoded",
      )
      .post(
        Map(
          "grant_type" -> "refresh_token",
          "refresh_token" -> refreshToken,
        )
      ).?|
    authToken <- response.body[JsValue].validate[AuthTokenResponse].?|
  } yield authToken

  case class AuthTokenResponse(
    expires_in: Int,
    token_type: String,
    scope: String,
    refresh_token: String,
    access_token: String,
    id_token: String
  )

  implicit val authTokenResponseFormats: OFormat[AuthTokenResponse] = Json.format[AuthTokenResponse]
}