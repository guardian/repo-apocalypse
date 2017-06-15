package com.gu.repoapocalypse

import com.amazonaws.serverless.proxy.internal.model.{ AwsProxyRequest, AwsProxyResponse }
import org.http4s._
import org.http4s.dsl._
import org.http4s.Cookie

object Lambda {
  import collection.JavaConverters._

  def respond(apiGatewayRequest: AwsProxyRequest): AwsProxyResponse = {
    val request = for {
      method <- Method.fromString(apiGatewayRequest.getHttpMethod)
    } yield Request(
      method = method,
      uri = Uri(path = apiGatewayRequest.getPath, query = Query.fromString(apiGatewayRequest.getQueryString)),
      headers = Headers(apiGatewayRequest.getHeaders.asScala.toList.map {
        case (k, v) => Header(k, v)
      }),
      body = fs2.Stream.emits(apiGatewayRequest.getBody.getBytes)
    )

    val response = service.run(request.right.get).unsafeRun().orNotFound

    val apiGatewayResponse = new AwsProxyResponse()
    apiGatewayResponse.setStatusCode(response.status.code)
    apiGatewayResponse.setHeaders(response.headers.toList.map(nv => nv.name.value -> nv.value).toMap.asJava)
    apiGatewayResponse
  }

  val service = HttpService {
    case request @ POST -> Root / "archive" => {
      request.decode[UrlForm] { formData =>
        formData.getFirst("repoName").map(repoName =>
          Archive.archive(repoName).fold(
            {
              case MissingParameterError(param) => BadRequest(s"Missing required param $param")
              case MissingEnvError(_) => InternalServerError()
              case UnexpectedExceptionError(context, _) => InternalServerError(s"Error during $context")
            },
            result => Ok(result)
          )).getOrElse(BadRequest(s"Missing required param 'repoName'"))
      }
    }
    case request @ GET -> Root / "callback" => {
      request.params.get("code").map(Auth.accessTokenFromSessionCode).map(_.flatMap(accessToken =>
        TemporaryRedirect(uri("/foo")).addCookie(
          Cookie(name = "access_token", content = accessToken, httpOnly = true)
        ))).getOrElse(BadRequest(s"Expected 'code' parameter"))
    }
  }
}