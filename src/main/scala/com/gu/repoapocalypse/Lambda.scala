package com.gu.repoapocalypse

import java.nio.file.{ Files, Path, Paths }
import java.util.zip.{ ZipEntry, ZipOutputStream }

import com.amazonaws.regions.Regions
import com.amazonaws.serverless.proxy.internal.model.{ AwsProxyRequest, AwsProxyResponse }
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.eclipse.jgit.api.Git

import scala.util.Try

object Lambda {
  val client = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build()
  val PARAM_NAME = "repoName"

  def respond(req: AwsProxyRequest): AwsProxyResponse = {
    val repoName = Option(req.getBody).flatMap { b =>
      b.split('=').toList match {
        case PARAM_NAME :: repoName :: Nil => Some(repoName)
        case _ => None
      }
    }.toRight(MissingParameterError(PARAM_NAME))

    val bucketName = sys.env.get("BUCKET_NAME").toRight(MissingEnvError("BUCKET_NAME"))

    val result = for {
      bucket <- bucketName
      repo <- repoName
      file <- archive(repo)
      uploadLocation <- upload(bucket, file)
    } yield uploadLocation

    result.fold(_.toProxyResponse, location => {
      val response = new AwsProxyResponse()
      response.setStatusCode(200)
      response.setBody(s"Archived to $location")
      response
    })
  }

  def upload(bucket: String, path: Path): Either[Error, String] = {
    val locationDescription = s"s3://$bucket/${path.getFileName}"
    Try {
      client.putObject(bucket, path.getFileName.toString, path.toFile)
    }.toEither
      .left.map(t => UnexpectedExceptionError(s"Uploading to $locationDescription", t))
      .map(_ => locationDescription)
  }

  def archive(repoName: String): Either[Error, Path] =
    cloneRepo(repoName).flatMap(zipAll(_, Paths.get(s"/tmp/${repoName}.zip")))

  def cloneRepo(repoName: String): Either[Error, Path] = {
    val gitURI = s"https://github.com/guardian/${repoName}.git"
    Try {
      val tmpLocation = Files.createTempDirectory("repo-apocalypse")
      Git.cloneRepository()
        .setBare(true)
        .setURI(gitURI)
        .setDirectory(tmpLocation.toFile)
        .call()
      tmpLocation
    }.toEither
      .left.map(t => UnexpectedExceptionError(s"Cloning Repo $gitURI", t))
  }

  def zipAll(baseDirectory: Path, zipLocation: Path): Either[Error, Path] = {
    Try {
      val zip = new ZipOutputStream(Files.newOutputStream(zipLocation))
      Files.walk(baseDirectory).filter(f => !Files.isDirectory(f)).forEach { file =>
        zip.putNextEntry(new ZipEntry(baseDirectory.relativize(file).toString))
        Files.copy(file, zip)
        zip.closeEntry()
      }
      zip.close()
      zipLocation
    }.toEither
      .left.map(t => UnexpectedExceptionError(s"Creating Zip", t))
  }
}