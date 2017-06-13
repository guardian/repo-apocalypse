package com.gu.repoapocalypse

import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse

sealed abstract class Error {
  def statusCode: Int
  def body: String
  def toProxyResponse: AwsProxyResponse = {
    val response = new AwsProxyResponse()
    response.setStatusCode(statusCode)
    response.setBody(body)
    response
  }
}

case class MissingParameterError(parameterName: String) extends Error {
  override val statusCode = 400
  override val body = s"Missing '$parameterName' parameter"
}
case class MissingEnvError(parameterName: String) extends Error {
  override val statusCode = 500
  override val body = s"Expected '$parameterName' as an environment variable"
}
case class UnexpectedExceptionError(context: String, t: Throwable) extends Error {
  override val statusCode = 500
  override val body = s"During $context, unexpectedly threw \n$t"
}

