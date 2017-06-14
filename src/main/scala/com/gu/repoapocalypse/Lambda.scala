package com.gu.repoapocalypse

import com.amazonaws.serverless.proxy.internal.model.{ AwsProxyRequest, AwsProxyResponse }

object Lambda {

  def respond(req: AwsProxyRequest): AwsProxyResponse = {
    (req.getPath, req.getHttpMethod) match {
      case ("/archive", "POST") => {
        val result = Archive.archive(Option(req.getBody))

        result.fold(_.toProxyResponse, location => {
          val response = new AwsProxyResponse()
          response.setStatusCode(200)
          response.setBody(s"Archived to $location")
          response
        })
      }
      case ("/callback", "GET") => Auth.callback(req)
      case (path, method) => {
        val response = new AwsProxyResponse()
        response.setStatusCode(404)
        response.setBody(s"Don't know how to handle a $method on $path")
        response
      }
    }
  }
}