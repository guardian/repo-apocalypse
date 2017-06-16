name := "repo-apocalypse"
description:= "github-project-archiver"

scalaVersion := "2.12.2"
organization := "com.gu"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-target:jvm-1.8",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint:-unused,_",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)

enablePlugins(RiffRaffArtifact, JavaAppPackaging, SbtTwirl)

topLevelDirectory in Universal := None
packageName in Universal := normalizedName.value

riffRaffPackageType := (dist in Universal).value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffManifestProjectName := "repo-apocalypse"

resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.amazonaws.serverless" % "aws-serverless-java-container-core" % "0.4",
  "org.http4s" %% "http4s-dsl" % "0.17.0-M1",
  "org.http4s" %% "http4s-server" % "0.17.0-M1",
  "org.http4s" %% "http4s-blaze-client" % "0.17.0-M1",
  "org.http4s" %% "http4s-circe" % "0.17.0-M1",
  "org.http4s" %% "http4s-twirl" % "0.17.0-M1",
  "io.circe" %% "circe-generic" % "0.8.0",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.144",
  "org.eclipse.jgit" % "org.eclipse.jgit" % "4.7.1.201706071930-r",
  "com.47deg" %% "github4s" % "0.15.0"
)

initialize := {
  val _ = initialize.value
  assert(sys.props("java.specification.version") == "1.8",
    "Java 8 is required for this project.")
}