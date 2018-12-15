package keys

import java.io.{File, FileInputStream, FileOutputStream}

trait KeysFileOps {
  def writeKey(path: String, key: Array[Byte])

  def isKeysDirExists(nodeName: String): Boolean

  def createKeysDir(nodeName: String): Unit

  def readKeyFromFile(fileName: String): Array[Byte]
}

object ProdKeysFileOps extends KeysFileOps {
  override def writeKey(path: String, key: Array[Byte]) = {
    val fos = new FileOutputStream(path)
    fos.write(key)
    fos.close()
  }

  override def isKeysDirExists(nodeName: String): Boolean = {
    val keyDir = new File(nodeName)
    keyDir.exists && keyDir.isDirectory
  }

  override def createKeysDir(nodeName: String) = {
    new File(nodeName).mkdirs()
  }

  override def readKeyFromFile(fileName: String) = {
    val fis = new FileInputStream(fileName)
    val size = new File(fileName).length()
    val buffer = Array.ofDim[Byte](size.toInt)
    fis.read(buffer)
    fis.close()
    buffer
  }

}

