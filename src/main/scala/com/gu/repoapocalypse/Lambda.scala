package com.gu.repoapocalypse

import java.nio.file.{ Files, Paths }
import java.util.zip.{ ZipEntry, ZipOutputStream }

import org.eclipse.jgit.api.Git

object Lambda {
  def archive(repoName: String): String = {
    val tmpLocation = Files.createTempDirectory("repo-apocalypse")
    Git.cloneRepository()
      .setBare(true)
      .setURI(s"https://github.com/guardian/${repoName}.git")
      .setDirectory(tmpLocation.toFile)
      .call()
    val zipLocation = Paths.get(s"/tmp/${repoName}.zip")
    val zip = new ZipOutputStream(Files.newOutputStream(zipLocation))
    Files.walk(tmpLocation).filter(f => !Files.isDirectory(f)).forEach { file =>
      zip.putNextEntry(new ZipEntry(tmpLocation.relativize(file).toString))
      Files.copy(file, zip)
      zip.closeEntry()

    }
    zip.close()
    zipLocation.toString
  }
}