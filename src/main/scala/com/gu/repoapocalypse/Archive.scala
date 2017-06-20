package com.gu.repoapocalypse

import java.nio.file.{Files, Path, Paths}
import java.util.zip.{ZipEntry, ZipOutputStream}

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import fs2.{Strategy, Task}
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

object Archive {
  val client = AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build()
  val PARAM_NAME = "repoName"

  def archive(bucketName: String, prefix: String, repoName: String, accessToken: String)(implicit S: Strategy): Task[String] = {
    for {
      cloneDirectory <- cloneRepo(repoName, accessToken)
      file <- zipAll(cloneDirectory, Paths.get(s"/tmp/${repoName}.zip"))
      uploadLocation <- upload(bucketName, prefix, file)
    } yield uploadLocation
  }

  def upload(bucket: String, prefix: String, path: Path)(implicit S: Strategy): Task[String] = {
    val trimmedPrefix = prefix.stripPrefix("/").stripSuffix("/")
    val prefixWithSlash = if (trimmedPrefix.isEmpty) "" else s"$trimmedPrefix/"
    val locationDescription = s"s3://$bucket/$prefixWithSlash${path.getFileName}"
    Task {
      client.putObject(bucket, s"$prefixWithSlash${path.getFileName}", path.toFile)
      locationDescription
    }
  }

  def cloneRepo(repoName: String, accessToken: String)(implicit S: Strategy): Task[Path] = {
    val gitURI = s"https://github.com/guardian/${repoName}.git"
    Task {
      val tmpLocation = Files.createTempDirectory("repo-apocalypse")
      Git.cloneRepository()
        .setBare(true)
        .setURI(gitURI)
        .setDirectory(tmpLocation.toFile)
        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken, ""))
        .call()
      tmpLocation
    }
  }

  def zipAll(baseDirectory: Path, zipLocation: Path)(implicit S: Strategy): Task[Path] = {
    Task {
      val zip = new ZipOutputStream(Files.newOutputStream(zipLocation))
      Files.walk(baseDirectory).filter(f => !Files.isDirectory(f)).forEach { file =>
        zip.putNextEntry(new ZipEntry(baseDirectory.relativize(file).toString))
        Files.copy(file, zip)
        zip.closeEntry()
      }
      zip.close()
      zipLocation
    }
  }
}
