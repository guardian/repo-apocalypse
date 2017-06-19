package com.gu.repoapocalypse

import java.nio.file.{Files, Path, Paths}
import java.util.zip.{ZipEntry, ZipOutputStream}

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

import scala.util.Try

object Archive {
  val client = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build()
  val PARAM_NAME = "repoName"

  def archive(bucketName: String, prefix: String, repoName: String, accessToken: String): Either[Error, String] = {
    for {
      cloneDirectory <- cloneRepo(repoName, accessToken)
      file <- zipAll(cloneDirectory, Paths.get(s"/tmp/${repoName}.zip"))
      uploadLocation <- upload(bucketName, prefix, file)
    } yield uploadLocation
  }

  def upload(bucket: String, prefix: String, path: Path): Either[Error, String] = {
    val trimmedPrefix = prefix.stripPrefix("/").stripSuffix("/")
    val prefixWithSlash = if (trimmedPrefix.isEmpty) "" else s"$trimmedPrefix/"
    val locationDescription = s"s3://$bucket/$prefixWithSlash${path.getFileName}"
    Try {
      client.putObject(bucket, s"$prefixWithSlash${path.getFileName}", path.toFile)
    }.toEither
      .left.map(t => UnexpectedExceptionError(s"Uploading to $locationDescription", t))
      .map(_ => locationDescription)
  }

  def cloneRepo(repoName: String, accessToken: String): Either[Error, Path] = {
    val gitURI = s"https://github.com/guardian/${repoName}.git"
    Try {
      val tmpLocation = Files.createTempDirectory("repo-apocalypse")
      Git.cloneRepository()
        .setBare(true)
        .setURI(gitURI)
        .setDirectory(tmpLocation.toFile)
        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken, ""))
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
