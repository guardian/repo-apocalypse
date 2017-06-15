package com.gu.repoapocalypse

import java.nio.file.{ Files, Path, Paths }
import java.util.zip.{ ZipEntry, ZipOutputStream }

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.eclipse.jgit.api.Git

import scala.util.Try

object Archive {
  val client = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build()
  val PARAM_NAME = "repoName"

  def archive(repoName: String): Either[Error, String] = {
    val bucketName = sys.env.get("BUCKET_NAME").toRight(MissingEnvError("BUCKET_NAME"))

    for {
      bucket <- bucketName
      cloneDirectory <- cloneRepo(repoName)
      file <- zipAll(cloneDirectory, Paths.get(s"/tmp/${repoName}.zip"))
      uploadLocation <- upload(bucket, file)
    } yield uploadLocation
  }

  def upload(bucket: String, path: Path): Either[Error, String] = {
    val locationDescription = s"s3://$bucket/${path.getFileName}"
    Try {
      client.putObject(bucket, path.getFileName.toString, path.toFile)
    }.toEither
      .left.map(t => UnexpectedExceptionError(s"Uploading to $locationDescription", t))
      .map(_ => locationDescription)
  }

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
