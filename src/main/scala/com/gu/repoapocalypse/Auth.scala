package com.gu.repoapocalypse

import com.amazonaws.serverless.proxy.internal.model.{ AwsProxyRequest, AwsProxyResponse }
import fs2.Task
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.client.blaze.SimpleHttp1Client
import org.http4s.dsl.uri
import org.http4s.{ Method, Query, Request }

object Auth {
  val httpClient = SimpleHttp1Client()

  case class GitHubAuthResponse(access_token: String)

  def accessTokenFromSessionCode(sessionCode: String): Task[String] = {
    httpClient.expect(Request(
      Method.POST,
      uri("https://github.com/login/oauth/access_token")
        .copy(query = Query.fromPairs(
          "client_id" -> sys.env("CLIENT_ID"),
          "client_secret" -> sys.env("CLIENT_SECRET"),
          "code" -> sessionCode
        ))
    ))(jsonOf[GitHubAuthResponse]).map(_.access_token)
  }

  def callback(req: AwsProxyRequest): AwsProxyResponse = {
    val sessionCode = req.getQueryStringParameters.get("code")

    val call = httpClient.expect(Request(
      Method.POST,
      uri("https://github.com/login/oauth/access_token")
        .copy(query = Query.fromPairs(
          "client_id" -> sys.env("CLIENT_ID"),
          "client_secret" -> sys.env("CLIENT_SECRET"),
          "code" -> sessionCode
        ))
    ))(jsonOf[GitHubAuthResponse])

    val result = call.unsafeRun
    val response = new AwsProxyResponse()
    response.setStatusCode(200)
    response.addHeader("Set-Cookie", s"access_token=${result.access_token}; HttpOnly; SameSite=Strict")
    response
  }

}
