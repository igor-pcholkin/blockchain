package core

import java.io.{File, FileOutputStream, PrintWriter}

import scala.io.Source

trait ChainFileOps {
  def isChainDirExists(nodeName: String): Boolean

  def createChainDir(nodeName: String): Unit

  def getChainDir(nodeName: String): String

  def writeBlock(i: Int, block: Block, chainDir: String): Unit

  def readBlock(i: Int, chainDir: String): Option[Block]

  def deleteBlock(i: Int, chainDir: String): Unit
}

object ProdChainFileOps extends ChainFileOps {

  override def getChainDir(nodeName: String): String = s"$nodeName/chain"

  override def isChainDirExists(nodeName: String): Boolean = {
    val chainDir = new File(getChainDir(nodeName))
    chainDir.exists && chainDir.isDirectory
  }

  override def createChainDir(nodeName: String): Unit = {
    val chainDir = getChainDir(nodeName)
    new File(chainDir).mkdirs()
  }

  override def writeBlock(i: Int, block: Block, chainDir: String): Unit = {
    val blockFile = new File(s"$chainDir/$i")
    if (!blockFile.exists()) {
      val pw = new PrintWriter(new FileOutputStream(blockFile.getAbsolutePath))
      pw.write(block.toString)
      pw.close()
    }
  }

  override def readBlock(i: Int, chainDir: String): Option[Block] = {
    val blockFile = new File(s"$chainDir/$i")
    if (blockFile.exists()) {
      val source = Source.fromFile(blockFile)
      val blockAsString = source.getLines().mkString
      source.close()
      Block.parse(blockAsString)
    } else {
      None
    }
  }

  override def deleteBlock(i: Int, chainDir: String): Unit = {
    new File(s"$chainDir/$i").delete()
  }
}