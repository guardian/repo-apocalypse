package com.gu.repoapocalypse

object Env {
  lazy val bucketName = sys.env.get("BUCKET_NAME").toRight(MissingEnvError("BUCKET_NAME"))
  lazy val s3pathPrefix = sys.env.get("PATH_PREFIX").toRight(MissingEnvError("PATH_PREFIX"))
  lazy val clientId = sys.env.get("CLIENT_ID").toRight(MissingEnvError("CLIENT_ID"))
  lazy val clientSecret = sys.env("CLIENT_SECRET")
}
