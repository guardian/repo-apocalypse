package com.gu.repoapocalypse

import cats.data.OptionT
import com.amazonaws.serverless.proxy.internal.model.{AwsProxyRequest, AwsProxyResponse}
import fs2.Task
import org.http4s._

object ApiGatewayHttp4sAdapter {
  import collection.JavaConverters._

  def apply(service: HttpService): (AwsProxyRequest => AwsProxyResponse) = { apiGatewayRequest =>
    val request = for {
      method <- Method.fromString(apiGatewayRequest.getHttpMethod)
    } yield Request(
      method = method,
      uri = Uri(
        path = apiGatewayRequest.getPath,
        query = Query.fromPairs(Option(apiGatewayRequest.getQueryStringParameters).map(_.asScala).getOrElse(Map.empty).toList: _*)
      ),
      headers = Headers(apiGatewayRequest.getHeaders.asScala.toList.map {
        case (k, v) => Header(k, v)
      }),
      body = Option(apiGatewayRequest.getBody).map(body => fs2.Stream.emits(body.getBytes)).getOrElse(EmptyBody)
    )

    import cats.instances.option._
    import cats.syntax.traverse._
    import fs2.interop.cats._
    (for {
      maybeResponse <- OptionT(request.toOption.map(service.run).sequence)
      response <- OptionT.fromOption[Task](maybeResponse.toOption)
      body <- OptionT(response.body.through(fs2.text.utf8Decode).runLast.map(_.orElse(Some(""))))
    } yield {
      val apiGatewayResponse = new AwsProxyResponse()
      apiGatewayResponse.setStatusCode(response.status.code)
      apiGatewayResponse.setHeaders(response.headers.toList.map(nv => nv.name.value -> nv.value).toMap.asJava)
      apiGatewayResponse.setBody(body)
      apiGatewayResponse
    }).value.unsafeRun().get
  }
}
