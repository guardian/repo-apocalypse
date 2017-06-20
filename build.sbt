name := "repo-apocalypse"
description:= "github-project-archiver"
organization := "com.gu"

scalaVersion in ThisBuild := "2.12.2"

scalacOptions in ThisBuild ++= Seq(
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

val http4sVersion = "0.17.0-M3"

lazy val root = (project in file(".")).enablePlugins(RiffRaffArtifact, JavaAppPackaging, SbtTwirl).settings(
  topLevelDirectory in Universal := None,
  packageName in Universal := normalizedName.value,

  riffRaffPackageType := (dist in Universal).value,
  riffRaffUploadArtifactBucket := Option("riffraff-artifact"),
  riffRaffUploadManifestBucket := Option("riffraff-builds"),
  riffRaffManifestProjectName := "repo-apocalypse",

  libraryDependencies ++= Seq(
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-server" % http4sVersion,
    "org.http4s" %% "http4s-blaze-client" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion,
    "org.http4s" %% "http4s-twirl" % http4sVersion,
    "io.circe" %% "circe-generic" % "0.8.0",
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.144",
    "org.eclipse.jgit" % "org.eclipse.jgit" % "4.7.1.201706071930-r"
  )
)

lazy val local = (project in file("local")).dependsOn(root).settings(
  libraryDependencies += "org.http4s" %% "http4s-blaze-server" % http4sVersion
)

initialize := {
  val _ = initialize.value
  assert(sys.props("java.specification.version") == "1.8",
    "Java 8 is required for this project.")
}