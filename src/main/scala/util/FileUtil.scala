package util

import java.io.{File, FileOutputStream, PrintWriter}
import scala.io.Source
import scala.util.{Failure, Success, Try}

trait FileOps {
  def readFile(fileName: String): Either[String, String]

  def writeFile(fileName: String, contents: String): Unit

  def createDirIfNotExists(dir: String): Unit
}

object ProdFileOps extends FileOps {
  override def readFile(fileName: String): Either[String, String] = Try {
    val s = Source.fromFile(fileName)
    val contents = s.getLines().mkString
    s.close
    contents
  } match {
    case Success(s) => Right(s)
    case Failure(_) => Left(s"Can't read $fileName")
  }

  override def writeFile(fileName: String, contents: String): Unit = {
    val pw = new PrintWriter(new FileOutputStream(fileName))
    pw.println(contents)
    pw.close()
  }

  override def createDirIfNotExists(dir: String): Unit = {
    val configDir = new File(dir)
    if (!configDir.exists()) {
      configDir.mkdirs
    }
  }

}
