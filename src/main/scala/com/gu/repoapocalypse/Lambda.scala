package com.gu.repoapocalypse

import java.nio.file.{ Files, Path, Paths }
import java.util.zip.{ ZipEntry, ZipOutputStream }

import com.amazonaws.serverless.proxy.internal.model.{ AwsProxyRequest, AwsProxyResponse }
import org.eclipse.jgit.api.Git

import scala.util.{ Failure, Success, Try }

object Lambda {
  def respond(req: AwsProxyRequest): AwsProxyResponse = {
    val maybeRepoName = Option(req.getBody).flatMap {
      b =>
        b.split('=').toList match {
          case "repoName" :: repoName :: Nil => Some(repoName)
          case _ => None
        }
    }
    val archiveResult = maybeRepoName.map { name =>
      Try { archive(name) }
    }
    val response = new AwsProxyResponse()
    archiveResult.fold {
      response.setStatusCode(400)
      response.setBody("Missing 'repoName' parameter")
    } {
      case Success(r) => {
        response.setStatusCode(200)
        response.setBody(s"Archived to $r")
      }
      case Failure(e) => {
        response.setStatusCode(500)
        response.setBody(e.toString)
      }
    }
    response
  }

  def archive(repoName: String): String = {
    val tmpLocation: Path = cloneRepo(repoName)
    zipAll(tmpLocation, Paths.get(s"/tmp/${repoName}.zip")).toString
  }

  def cloneRepo(repoName: String): Path = {
    val tmpLocation = Files.createTempDirectory("repo-apocalypse")
    Git.cloneRepository()
      .setBare(true)
      .setURI(s"https://github.com/guardian/${repoName}.git")
      .setDirectory(tmpLocation.toFile)
      .call()
    tmpLocation
  }

  def zipAll(baseDirectory: Path, zipLocation: Path): Path = {
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