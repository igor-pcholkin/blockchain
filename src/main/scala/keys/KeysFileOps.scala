package keys

import java.io.{File, FileOutputStream, PrintWriter}

import keys.ProdKeysFileOps.keyDir

import scala.io.Source

trait KeysFileOps {
  def writeKey(path: String, key: String)

  def isKeysDirExists(nodeName: String): Boolean

  def createKeysDir(nodeName: String): Unit

  def readKeyFromFile(fileName: String): String

  def getUserByKey(encodedPublicKey: String): Option[String]
}

object ProdKeysFileOps extends KeysFileOps {
  def keyDir() = new File("keys")

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

  def isKeysDirExists(): Boolean = {
    val kd = keyDir()
    kd.exists && kd.isDirectory
  }

  def createKeysDir() = {
    keyDir().mkdirs()
  }

  override def readKeyFromFile(fileName: String) = {
    val s = Source.fromFile(fileName)
    val key = s.getLines().mkString
    s.close()
    key
  }

  override def getUserByKey(encodedPublicKey: String) = {
    if (!isKeysDirExists()) {
      createKeysDir
    }
    keyDir().list.toStream.map { userName =>
      (userName, readKeyFromFile(s"keys/$userName/publicKey"))
    }.find(_._2 == encodedPublicKey).map(_._1)
  }
}

