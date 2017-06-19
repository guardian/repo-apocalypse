package com.gu.repoapocalypse

object Env {
  val bucketName = sys.env.get("BUCKET_NAME").toRight(MissingEnvError("BUCKET_NAME"))
  val s3pathPrefix = sys.env.get("PATH_PREFIX").toRight(MissingEnvError("PATH_PREFIX"))
  val clientId = sys.env("CLIENT_ID")
  val clientSecret = sys.env("CLIENT_SECRET")
}
