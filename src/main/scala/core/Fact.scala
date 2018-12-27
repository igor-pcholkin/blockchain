package core

import keys.KeysFileOps

abstract class Statement {
  def publicKeysRequiredToSignEncoded: Seq[String]
  def providedSignaturesForKeys: Seq[(String, String)]

  def dataToSign: Array[Byte] = Array[Byte]()

  def couldBeSignedByLocalPublicKey(nodeName: String, keysFileOps: KeysFileOps): Boolean = {
    publicKeysRequiredToSignEncoded.filterNot { publicKey =>
      providedSignaturesForKeys.exists(_._1 == publicKey)
    }.exists { publicKey =>
      keysFileOps.getUserByKey(nodeName, publicKey).nonEmpty
    }
  }
}

abstract class Fact extends Statement