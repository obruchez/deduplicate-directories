package org.bruchez.olivier.deduplicatedirectories

import java.nio.file._
import java.security.MessageDigest
import scala.jdk.CollectionConverters._
import scala.util.{Try, Using}

object DeduplicateDirectories {
  case class FileInfo(path: Path) {
    lazy val size: Long = Files.size(path)
    lazy val md5: String = DeduplicateDirectories.md5(path)
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 2 || args.length > 3) {
      println("Usage: FileOrganizer <reference_path> <target_path> [check_names]")
      println("  check_names: Optional boolean flag to also check filenames (default: false)")
      System.exit(1)
    }

    val referencePath = Paths.get(args(0))
    val targetPath = Paths.get(args(1))
    val checkNames = args.length > 2 && args(2).toBoolean

    if (!Files.exists(referencePath) || !Files.exists(targetPath)) {
      println("Both paths must exist!")
      System.exit(1)
    }

    println(s"Processing files...")
    println(s"Reference path: $referencePath")
    println(s"Target path: $targetPath")
    println(s"Checking filenames: $checkNames")

    val okPath = targetPath.resolve("OK")
    val nokPath = targetPath.resolve("NOK")
    Files.createDirectories(okPath)
    Files.createDirectories(nokPath)

    processDirectories(referencePath, targetPath, okPath, nokPath, checkNames)
  }

  private def processDirectories(referencePath: Path, targetPath: Path, okPath: Path, nokPath: Path, checkNames: Boolean): Unit = {
    val referenceFiles = allFilesIn(referencePath).map(FileInfo)

    allFilesIn(targetPath).foreach { targetFile =>
      if (!targetFile.startsWith(okPath) && !targetFile.startsWith(nokPath)) {
        val targetInfo = FileInfo(targetFile)

        val exists = referenceFiles.exists { refFile =>
          (!checkNames || refFile.path.getFileName == targetFile.getFileName) &&
            refFile.size == targetInfo.size &&
            refFile.md5 == targetInfo.md5
        }

        val relativePath = targetPath.relativize(targetFile)
        val destinationRoot = if (exists) okPath else nokPath
        val destinationPath = destinationRoot.resolve(relativePath)

        Files.createDirectories(destinationPath.getParent)

        try {
          Files.move(targetFile, destinationPath, StandardCopyOption.REPLACE_EXISTING)
          println(s"Moved ${targetFile.getFileName} to ${if (exists) "OK" else "NOK"} folder")
        } catch {
          case e: Exception =>
            println(s"Error moving file ${targetFile.getFileName}: ${e.getMessage}")
        }
      }
    }
  }

  private def allFilesIn(dir: Path): Seq[Path] = {
    if (Files.isDirectory(dir)) {
      Using(Files.walk(dir)) { stream =>
        stream.iterator().asScala
          .filter(Files.isRegularFile(_))
          .toSeq
      }.getOrElse(Seq.empty)
    } else {
      Seq.empty
    }
  }

  private def md5(file: Path): String = {
    Try {
      val md = MessageDigest.getInstance("MD5")
      val bytes = Files.readAllBytes(file)
      md.update(bytes)
      md.digest().map("%02x".format(_)).mkString
    }.getOrElse("")
  }
}