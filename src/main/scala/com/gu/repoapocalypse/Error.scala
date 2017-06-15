package com.gu.repoapocalypse

sealed abstract class Error
case class MissingEnvError(parameterName: String) extends Error
case class UnexpectedExceptionError(context: String, t: Throwable) extends Error

