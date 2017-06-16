package com.gu.repoapocalypse

import com.amazonaws.serverless.proxy.internal.model.{AwsProxyRequest, AwsProxyResponse}
import org.http4s._
import org.http4s.dsl._
import org.http4s.twirl._

object Lambda {

  def respond(apiGatewayRequest: AwsProxyRequest): AwsProxyResponse = {
    ApiGatewayHttp4sAdapter(service)(apiGatewayRequest)
  }

  val noAuthService = HttpService {
    case GET -> Root / "login" => {
      Ok(html.login(sys.env("CLIENT_ID")))
    }

    case request @ GET -> Root / "callback" => {
      request.params.get("code").map(Auth.accessTokenFromSessionCode).map(_.flatMap(accessToken =>
        TemporaryRedirect(uri("/CODE/form")).addCookie(
          Cookie(name = "access_token", content = accessToken, httpOnly = true)
        ))).getOrElse(BadRequest(s"Expected 'code' parameter"))
    }
  }

  val serviceRequiringToken: AuthedService[Session] = AuthedService {
    case AuthedRequest(_, GET -> Root / "form") => {
      Ok(html.form())
    }
    case AuthedRequest(session, request @ POST -> Root / "archive") => {
      request.decode[UrlForm] { formData =>
        formData.getFirst("repoName").map(repoName =>
          Archive.archive(repoName, session.accessToken).fold(
            {
              case MissingEnvError(_) => InternalServerError()
              case UnexpectedExceptionError(context, _) => InternalServerError(s"Error during $context")
            },
            result => Ok(result)
          )).getOrElse(BadRequest(s"Missing required param 'repoName'"))
      }
    }
  }

  import cats.syntax.semigroup._
  val service = noAuthService |+| Auth.middleware(serviceRequiringToken)
}