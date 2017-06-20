package com.gu.repoapocalypse

import fs2.{Strategy, Task}
import org.http4s._
import org.http4s.dsl._
import org.http4s.twirl._

object Lambda {

  def respond(apiGatewayRequest: ApiGatewayRequest): ApiGatewayResponse = {
    ApiGatewayHttp4sAdapter(service)(apiGatewayRequest)
  }

  val noAuthService = HttpService {
    case GET -> Root / "login" => {
      Env.clientId.fold(err => InternalServerError(err.toString), clientId =>
        Ok(html.login(clientId))
      )
    }

    case request @ GET -> Root / "callback" => {
      val stage = request.attributes.get(ApiGatewayHttp4sAdapter.stageKey).getOrElse("")
      val redirectURI = Uri.fromString(s"/$stage/form").right.get
      Env.clientId.fold(err => InternalServerError(err.toString), clientId =>
        request.params.get("code")
          .map(sessionCode => Auth.accessTokenFromSessionCode(sessionCode, clientId, Env.clientSecret))
          .map(_.flatMap(accessToken =>
            TemporaryRedirect(redirectURI).addCookie(
              Cookie(name = "access_token", content = accessToken, httpOnly = true)
            )
          )).getOrElse(BadRequest(s"Expected 'code' parameter"))
      )
    }
  }

  val serviceRequiringToken: AuthedService[Session] = AuthedService {
    case AuthedRequest(_, request @ GET -> Root / "form") => {
      Ok(html.form(request.attributes(ApiGatewayHttp4sAdapter.stageKey)))
    }
    case AuthedRequest(session, request @ POST -> Root / "archive") => {
      request.decode[UrlForm] { formData =>
        formData.getFirst("repoName").map[Task[Response]](repoName =>
          (for {
            bucket <- Env.bucketName
            prefix <- Env.s3pathPrefix
          } yield
            Archive.archive(bucket, prefix, repoName, session.accessToken)(Strategy.sequential).attempt
              .flatMap[Response](_.fold(
                t => InternalServerError(t.toString),
                location => Ok(location)
            ))
          ).getOrElse(InternalServerError("Missing environment variable"))
        ).getOrElse(BadRequest(s"Missing required param 'repoName'"))
      }
    }
  }

  import cats.syntax.semigroup._
  val service = noAuthService |+| Auth.middleware(serviceRequiringToken)
}