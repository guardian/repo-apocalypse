package com.gu.repoapocalypse

import org.http4s._
import org.http4s.dsl._
import org.http4s.twirl._

object Lambda {

  def respond(apiGatewayRequest: ApiGatewayRequest): ApiGatewayResponse = {
    ApiGatewayHttp4sAdapter(service)(apiGatewayRequest)
  }

  val noAuthService = HttpService {
    case GET -> Root / "login" => {
      Ok(html.login(Env.clientId))
    }

    case request @ GET -> Root / "callback" => {
      val stage = request.attributes(ApiGatewayHttp4sAdapter.stageKey)
      val redirectURI = Uri.fromString(s"/$stage/form").right.get
      request.params.get("code")
        .map(sessionCode => Auth.accessTokenFromSessionCode(sessionCode, Env.clientId, Env.clientSecret))
        .map(_.flatMap(accessToken =>
          TemporaryRedirect(redirectURI).addCookie(
            Cookie(name = "access_token", content = accessToken, httpOnly = true)
          )
        )).getOrElse(BadRequest(s"Expected 'code' parameter"))
    }
  }

  val serviceRequiringToken: AuthedService[Session] = AuthedService {
    case AuthedRequest(_, request @ GET -> Root / "form") => {
      Ok(html.form(request.attributes(ApiGatewayHttp4sAdapter.stageKey)))
    }
    case AuthedRequest(session, request @ POST -> Root / "archive") => {
      request.decode[UrlForm] { formData =>
        formData.getFirst("repoName").map(repoName =>
          (for {
            bucket <- Env.bucketName
            prefix <- Env.s3pathPrefix
            result <- Archive.archive(bucket, prefix, repoName, session.accessToken)
          } yield result
          ).fold(
            {
              case MissingEnvError(_) => InternalServerError()
              case UnexpectedExceptionError(context, _) => InternalServerError(s"Error during $context")
            },
            success => Ok(success)
          )
        ).getOrElse(BadRequest(s"Missing required param 'repoName'"))
      }
    }
  }

  import cats.syntax.semigroup._
  val service = noAuthService |+| Auth.middleware(serviceRequiringToken)
}