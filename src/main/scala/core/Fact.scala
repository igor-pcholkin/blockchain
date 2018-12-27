package core

import keys.KeysFileOps
import messages.PaymentTransaction.bytesToBase64Str

abstract class Statement {
  def publicKeysRequiredToSignEncoded: Seq[String]
  def providedSignaturesForKeys: Seq[(String, String)]

  def dataToSign: Array[Byte]

  def addSignature(publicKey: String, signature: String): Statement

  def couldBeSignedByLocalPublicKey(nodeName: String, keysFileOps: KeysFileOps): Boolean = {
    notUsedPublicKeys.exists { publicKey =>
      keysFileOps.getUserByKey(nodeName, publicKey).nonEmpty
    }
  }

  def signByLocalPublicKeys(nodeName: String, keysFileOps: KeysFileOps): Unit = {
    val signer = new Signer(keysFileOps)
    notUsedPublicKeys foreach { publicKey =>
      keysFileOps.getUserByKey(nodeName, publicKey) map { userName =>
        signByUserPublicKey(nodeName, signer, userName, publicKey)
      }
    }
  }

  def signByUserPublicKey(nodeName: String, signer: Signer, userName: String, publicKeyEncoded: String): Statement = {
    val signature = signer.sign(nodeName, userName, publicKeyEncoded, dataToSign)
    val encodedSignature = bytesToBase64Str(signature)
    addSignature(publicKeyEncoded, encodedSignature)
  }

  private def notUsedPublicKeys = {
    publicKeysRequiredToSignEncoded.filterNot { publicKey =>
      providedSignaturesForKeys.exists(_._1 == publicKey)
    }
  }
}

abstract class Fact extends Statement