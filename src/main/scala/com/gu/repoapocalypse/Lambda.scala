package com.gu.repoapocalypse

import com.amazonaws.serverless.proxy.internal.model.{ AwsProxyRequest, AwsProxyResponse }
import org.http4s._
import org.http4s.dsl._
import org.http4s.Cookie
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

object ApiGatewayHttp4sAdapter {
  import collection.JavaConverters._

  def apply(service: HttpService): (AwsProxyRequest => AwsProxyResponse) = { apiGatewayRequest =>
    val request = for {
      method <- Method.fromString(apiGatewayRequest.getHttpMethod)
    } yield Request(
      method = method,
      uri = Uri(
        path = apiGatewayRequest.getPath,
        query = Query.fromString(apiGatewayRequest.getQueryString.stripPrefix("?"))
      ),
      headers = Headers(apiGatewayRequest.getHeaders.asScala.toList.map {
        case (k, v) => Header(k, v)
      }),
      body = Option(apiGatewayRequest.getBody).map(body => fs2.Stream.emits(body.getBytes)).getOrElse(EmptyBody)
    )

    val response = service.run(request.right.get).unsafeRun().orNotFound

    val apiGatewayResponse = new AwsProxyResponse()
    apiGatewayResponse.setStatusCode(response.status.code)
    apiGatewayResponse.setHeaders(response.headers.toList.map(nv => nv.name.value -> nv.value).toMap.asJava)
    apiGatewayResponse.setBody(response.body.through(fs2.text.utf8Decode).runLast.unsafeRun().getOrElse(""))
    apiGatewayResponse
  }
}