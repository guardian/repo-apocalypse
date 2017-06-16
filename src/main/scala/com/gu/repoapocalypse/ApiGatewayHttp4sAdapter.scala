package com.gu.repoapocalypse

import cats.data.OptionT
import fs2.Task
import org.http4s._

import scala.beans.BeanProperty

object ApiGatewayHttp4sAdapter {
  def apply(service: HttpService): (ApiGatewayRequest => ApiGatewayResponse) = { apiGatewayRequest =>
    val request = for {
      method <- Method.fromString(apiGatewayRequest.httpMethod)
    } yield Request(
      method = method,
      uri = Uri(
        path = apiGatewayRequest.path,
        query = Query.fromPairs(apiGatewayRequest.queryStringParamMap.toList: _*)
      ),
      headers = Headers(apiGatewayRequest.headerMap.toList.map {
        case (k, v) => Header(k, v)
      }),
      body = Option(apiGatewayRequest.body).map(body => fs2.Stream.emits(body.getBytes)).getOrElse(EmptyBody)
    )

    import collection.JavaConverters._
    import cats.instances.option._
    import cats.syntax.traverse._
    import fs2.interop.cats._
    (for {
      maybeResponse <- OptionT(request.toOption.map(service.run).sequence)
      response <- OptionT.fromOption[Task](maybeResponse.toOption)
      body <- OptionT(response.body.through(fs2.text.utf8Decode).runLast.map(_.orElse(Some(""))))
    } yield {
      new ApiGatewayResponse(
        statusCode = response.status.code,
        headers = response.headers.toList.map(nv => nv.name.value -> nv.value).toMap.asJava,
        body = body
      )
    }).value.unsafeRun().get
  }
}

class ApiGatewayResponse(
  @BeanProperty var statusCode: Int,
  @BeanProperty var headers: java.util.Map[String, String],
  @BeanProperty var body: String
)
class ApiGatewayRequest {
  @BeanProperty var httpMethod: String = null
  @BeanProperty var path: String = null
  @BeanProperty var queryStringParameters: java.util.Map[String, String] = null
  @BeanProperty var headers: java.util.Map[String, String] = null
  @BeanProperty var body: String = null
  @BeanProperty var base64Encoded: Boolean = false
  @BeanProperty var stageVariables: java.util.Map[String, String] = null

  import collection.JavaConverters._
  def asScalaMap[K,V](m: java.util.Map[K, V]): Map[K, V] = Option(m).map(_.asScala.toMap).getOrElse(Map.empty)
  def queryStringParamMap = asScalaMap(queryStringParameters)
  def headerMap = asScalaMap(headers)
}
