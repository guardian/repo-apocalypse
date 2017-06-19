package com.gu.repoapocalypse

import cats.data.Kleisli
import fs2.Task
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.client.blaze.SimpleHttp1Client
import org.http4s.dsl._
import org.http4s.headers.Cookie
import org.http4s.server.AuthMiddleware
import org.http4s._

object Auth {
  val httpClient = SimpleHttp1Client()

  case class GitHubAuthResponse(access_token: String)

  def accessTokenFromSessionCode(sessionCode: String, clientID: String, clientSecret: String): Task[String] = {
    httpClient.expect(Request(
      Method.POST,
      uri("https://github.com/login/oauth/access_token")
        .copy(query = Query.fromPairs(
          "client_id" -> clientID,
          "client_secret" -> clientSecret,
          "code" -> sessionCode
        ))
    ))(jsonOf[GitHubAuthResponse]).map(_.access_token)
  }

  val authUser: Service[Request, Either[String, Session]] = Kleisli(req =>
    Task.now(
      req.headers.get(Cookie)
        .flatMap(_.values.find(_.name == "access_token"))
        .map(c => Session(c.content))
        .toRight("Missing access_token")
    ))

  val onFailure: AuthedService[String] = Kleisli(_ => TemporaryRedirect(uri("/login")))

  val middleware = AuthMiddleware(authUser, onFailure)
}
case class Session(accessToken: String)