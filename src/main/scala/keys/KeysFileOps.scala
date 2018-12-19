package keys

import java.io.{File, FileOutputStream, PrintWriter}

import scala.io.Source

trait KeysFileOps {
  def writeKey(path: String, key: String)

  def isKeysDirExists(nodeName: String): Boolean

  def createKeysDir(nodeName: String): Unit

  def readKeyFromFile(fileName: String): String
}

object ProdKeysFileOps extends KeysFileOps {
  override def writeKey(path: String, key: String) = {
    val pw = new PrintWriter(new FileOutputStream(path))
    pw.write(key)
    pw.close()
  }

  override def isKeysDirExists(nodeName: String): Boolean = {
    val keyDir = new File(nodeName)
    keyDir.exists && keyDir.isDirectory
  }

  override def createKeysDir(nodeName: String) = {
    new File(nodeName).mkdirs()
  }

  override def readKeyFromFile(fileName: String) = {
    val s = Source.fromFile(fileName)
    val key = s.getLines().mkString
    s.close()
    key
  }

}

