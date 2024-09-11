package gg.sina.schwab

import com.typesafe.config.ConfigFactory
import gg.sina.schwab.models.market_data_api.ErrorResponse
import zio.*
import zio.config.typesafe.*
import zio.config.magnolia.*
import zio.connect.file.*
import zio.http.*
import zio.json.*
import zio.stream.*

import java.net.{URLDecoder, URLEncoder}


case class SchwabAuth private(
  private val accessToken: String,
  private val refreshToken: String,
  private val client: Client,
  private val fileConnector: FileConnector,
  private val config: SchwabAuth.SchwabAuthConfig,
) {
  def request(req: Request): RIO[Scope, (Response, SchwabAuth)] = request(req, true)
  private def request(req: Request, retryWithRefresh: Boolean): RIO[Scope, (Response, SchwabAuth)] = for {
    response <- client.request(req.addHeader("Authorization", f"Bearer $accessToken"))
    result <- response.status.code match {
      case 200 => ZIO.succeed(response, this)
      case 401 if retryWithRefresh => refresh.flatMap(_.request(req, retryWithRefresh = false))
      case _ => response.body.asString
        .flatMap(body => ZIO.fromEither(body.fromJson[ErrorResponse]).mapError(Exception(_)).flatMap(ZIO.fail))
    }
  } yield result

  protected def refresh: RIO[Scope, SchwabAuth] = for {
    response <- client.request(config.getRefreshRequest(refreshToken))
    body <- response.body.asString
    authTokenResponse <- ZIO.fromEither(body.fromJson[SchwabAuth.AuthTokenResponse]).mapError(Exception(_))
    _ <- ZStream
      .succeed(authTokenResponse.refresh_token)
      .via(ZPipeline.utf8Encode)
      .run(fileConnector.writeFileName(config.refresh_token_file))
    _ <- ZIO.log(f"Saved new refresh token to file (${config.refresh_token_file})")
  } yield copy(accessToken = authTokenResponse.access_token, refreshToken = authTokenResponse.refresh_token)
}

object SchwabAuth {
  private case class SchwabAuthConfig(app_key: String, secret: String, refresh_token_file: String) {
    val redirectUri: String = "https://127.0.0.1"
    val clientAuthUrl: String = "https://api.schwabapi.com/v1/oauth/authorize?" + Map(
      "client_id" -> app_key,
      "redirect_uri" -> redirectUri
    )
      .map { case (k, v) => f"${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }
      .mkString("&")

    def getRefreshRequest(refreshToken: String): Request = Request
      .post(
        "https://api.schwabapi.com/v1/oauth/token",
        Body.fromURLEncodedForm(Form(
          FormField.simpleField("grant_type", "refresh_token"),
          FormField.simpleField("refresh_token", refreshToken),
        ))
      )
      .addHeaders(
        Headers(
          Header.Authorization.Basic(app_key, secret),
          Header.ContentType(MediaType.application.`x-www-form-urlencoded`),
        )
      )
  }

  private def auth(client: Client, fileConnector: FileConnector, config: SchwabAuthConfig): ZIO[Scope, Throwable, AuthTokenResponse] =
    refresh(client, fileConnector, config) <> initialAuth(client, fileConnector, config)
  private def refresh(client: Client, fileConnector: FileConnector, config: SchwabAuthConfig): ZIO[Scope, Throwable, AuthTokenResponse] = for {
    _ <- ZIO.log("Authing")
    _ <- ZIO.log("Reading refreshToken from file")
    refreshToken <- fileConnector.readFileName(config.refresh_token_file).via(ZPipeline.utf8Decode).mkString
    _ <- ZIO.log("Refreshing access token")
    response <- client.request(
      Request.post(
          "https://api.schwabapi.com/v1/oauth/token",
          Body.fromURLEncodedForm(Form(
            FormField.simpleField("grant_type", "refresh_token"),
            FormField.simpleField("refresh_token", refreshToken),
          ))
        )
        .addHeaders(Headers(
          Header.Authorization.Basic(config.app_key, config.secret),
          Header.ContentType(MediaType.application.`x-www-form-urlencoded`),
        ))
    )
    body <- response.body.asString
    authTokenResponse <- ZIO.fromEither(body.fromJson[AuthTokenResponse]).mapError(Exception(_))
    _ <- ZStream.succeed(refreshToken).via(ZPipeline.utf8Encode).run(fileConnector.writeFileName(config.refresh_token_file))
    _ <- ZIO.log(f"Saved new refresh token to file (${config.refresh_token_file})")
  } yield authTokenResponse
  private def initialAuth(client: Client, fileConnector: FileConnector, config: SchwabAuthConfig): RIO[Scope, AuthTokenResponse] = for {
    _ <- Console.printLine(f"To Log Into Schwab Click Here: ${config.clientAuthUrl}")
    authCode <- Console.readLine("Paste authorization code: ").map(URLDecoder.decode(_, "UTF-8"))
    response <- client.request(
      Request.post(
          "https://api.schwabapi.com/v1/oauth/token",
          Body.fromURLEncodedForm(Form(
            FormField.simpleField("grant_type", "authorization_code"),
            FormField.simpleField("code", authCode),
            FormField.simpleField("redirect_uri", config.redirectUri),
          ))
        )
        .addHeaders(Headers(
          Header.ContentType(MediaType.application.`x-www-form-urlencoded`),
          Header.Authorization.Basic(config.app_key, config.secret),
        ))
    )
    body <- response.body.asString
    authTokenResponse <- ZIO.fromEither(body.fromJson[AuthTokenResponse]).mapError(Exception(_))
    _ <- ZStream
      .succeed(authTokenResponse.refresh_token)
      .via(ZPipeline.utf8Encode)
      .run(fileConnector.writeFileName(config.refresh_token_file))
    _ <- ZIO.log(f"Saved new refresh token to file (${config.refresh_token_file})")
  } yield authTokenResponse

  val layer: ZLayer[Scope & FileConnector & Client, Throwable, SchwabAuth] = ZLayer {
    for {
      client <- ZIO.service[Client]
      fileConnector <- ZIO.service[FileConnector]
      config <- TypesafeConfigProvider
        .fromTypesafeConfig(ConfigFactory.load().getConfig("schwab"))
        .load(deriveConfig[SchwabAuthConfig])
      authTokenResponse <- auth(client, fileConnector, config)
    } yield SchwabAuth(authTokenResponse.access_token, authTokenResponse.refresh_token, client, fileConnector, config)
  }
  case class AuthTokenResponse(
    expires_in: Int,
    token_type: String,
    scope: String,
    refresh_token: String,
    access_token: String,
    id_token: String
  )
  implicit val authTokenResponseDecoder: JsonDecoder[AuthTokenResponse] = DeriveJsonDecoder.gen[AuthTokenResponse]
}
