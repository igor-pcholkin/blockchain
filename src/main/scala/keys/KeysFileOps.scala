package keys

import java.io.{File, FileOutputStream, PrintWriter}

import scala.io.Source

trait KeysFileOps {
  def writeKeyToFile(nodeName: String, userName: String, keyName: String, key: String): Unit

  def readKeyFromFile(nodeName: String, userName: String, keyName: String): String

  def getUserByKey(nodeName: String, encodedPublicKey: String): Option[String]

  def isKeysDirExists(nodeName: String, userName: String): Boolean
}

object ProdKeysFileOps extends KeysFileOps {
  private def getKeysDir(nodeName: String) = new File(s"$nodeName/keys")
  private def getUserKeysDir(nodeName: String, userName: String) = s"$nodeName/keys/$userName"

  override def writeKeyToFile(nodeName: String, userName: String, keyName: String, key: String): Unit = {
    val keysDir = getUserKeysDir(nodeName, userName)
    if (!isKeysDirExists(nodeName, userName)) {
      createKeysDir(nodeName, userName)
    }
    val keyFileName = s"$keysDir/$keyName"
    val pw = new PrintWriter(new FileOutputStream(keyFileName))
    pw.write(key)
    pw.close()
  }

  def isKeysDirExists(nodeName: String, userName: String): Boolean = {
    val keysDir = new File(getUserKeysDir(nodeName, userName))
    keysDir.exists && keysDir.isDirectory
  }

  private def createKeysDir(nodeName: String, userName: String) = {
    new File(getUserKeysDir(nodeName, userName)).mkdirs()
  }

  private def isKeysDirExists(nodeName: String): Boolean = {
    val kd = getKeysDir(nodeName)
    kd.exists && kd.isDirectory
  }

  override def readKeyFromFile(nodeName: String, userName: String, keyName: String): String = {
    val keysDir = getUserKeysDir(nodeName, userName)
    val keyFileName = s"$keysDir/$keyName"
    val s = Source.fromFile(keyFileName)
    val key = s.getLines().mkString
    s.close()
    key
  }

  override def getUserByKey(nodeName: String, encodedPublicKey: String): Option[String] = {
    if (!isKeysDirExists(nodeName)) {
      None
    } else {
      getKeysDir(nodeName).list.toStream.map { userName =>
        (userName, readKeyFromFile(nodeName, userName, "publicKey"))
      }.find(_._2 == encodedPublicKey).map(_._1)
    }
  }
}

