package keys

import java.io.{File, FileOutputStream, PrintWriter}

import scala.io.Source

trait KeysFileOps {
  def writeKey(path: String, key: String)

  def isKeysDirExists(nodeName: String): Boolean

  def createKeysDir(nodeName: String): Unit

  def readKeyFromFile(fileName: String): String

  def getUserByKey(encodedPublicKey: String): Option[String]
}

object ProdKeysFileOps extends KeysFileOps {
  override def writeKey(path: String, key: String) = {
    val pw = new PrintWriter(new FileOutputStream(path))
    pw.write(key)
    pw.close()
  }

  override def isKeysDirExists(userName: String): Boolean = {
    val keyDir = new File(s"keys/$userName")
    keyDir.exists && keyDir.isDirectory
  }

  override def createKeysDir(userName: String) = {
    new File(s"keys/$userName").mkdirs()
  }

  override def readKeyFromFile(fileName: String) = {
    val s = Source.fromFile(fileName)
    val key = s.getLines().mkString
    s.close()
    key
  }

  override def getUserByKey(encodedPublicKey: String) = {
    val keyDir = new File("keys")
    keyDir.list.toStream.map { userDir =>
      readKeyFromFile("keys/$userDir/publicKey")
    }.find(_ == encodedPublicKey)
  }
}

