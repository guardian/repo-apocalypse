package com.gu.repoapocalypse

import org.http4s.server.blaze._
import fs2.Task
import org.http4s.util.StreamApp

object LocalServer extends StreamApp {

  override def stream(args: List[String]): fs2.Stream[Task, Nothing] =
    BlazeBuilder.bindHttp(8080, "localhost").mountService(Lambda.service, "/").serve
}
