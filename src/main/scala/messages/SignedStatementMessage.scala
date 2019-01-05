package messages

import core._
import keys.KeysFileOps
import util.StringConverter

object SignedStatementMessage {

  def apply(statement: Statement, publicKeysRequiredToSignEncoded: Seq[String],
            nodeName: String, keysFileOps: KeysFileOps): SignedStatementMessage = {
    val signedStatement = new SignedStatementMessage(statement, publicKeysRequiredToSignEncoded)
    val newSignatures = signedStatement.signByLocalPublicKeys(nodeName, keysFileOps)
    signedStatement.addSignatures(newSignatures)
  }
}

/**
  * SignedStatement is a wrapper which attaches signatures to statements.
  * As opposed to facts, statements are not stored in blockchain.
  */
case class SignedStatementMessage(statement: Statement, publicKeysRequiredToSignEncoded: Seq[String], providedSignaturesForKeys: Seq[(String, String)] = Nil)
  extends Message with StringConverter {

  def addSignature(publicKey: String, signature: String): SignedStatementMessage = {
    copy(providedSignaturesForKeys = providedSignaturesForKeys :+ (publicKey, signature))
  }

  def addSignatures(signatures: Seq[(String, String)]): SignedStatementMessage = {
    copy(providedSignaturesForKeys = providedSignaturesForKeys ++ signatures)
  }

  def couldBeSignedByLocalPublicKey(nodeName: String, keysFileOps: KeysFileOps): Boolean = {
    notUsedPublicKeys.exists { publicKey =>
      keysFileOps.getUserByKey(nodeName, publicKey).nonEmpty
    }
  }

  def signByLocalPublicKeys(nodeName: String, keysFileOps: KeysFileOps): Seq[(String, String)] = {
    val signer = new Signer(keysFileOps)
    notUsedPublicKeys flatMap { publicKey =>
      keysFileOps.getUserByKey(nodeName, publicKey) map { userName =>
        publicKey -> signByUserPublicKey(nodeName, signer, userName, publicKey)
      }
    }
  }

  def signByUserPublicKey(nodeName: String, signer: Signer, userName: String, publicKeyEncoded: String): String = {
    val signature = signer.sign(nodeName, userName, publicKeyEncoded, statement.dataToSign)
    bytesToBase64Str(signature)
  }

  def isSignedByAllKeys: Boolean = {
    notUsedPublicKeys.isEmpty
  }

  private def notUsedPublicKeys = {
    publicKeysRequiredToSignEncoded.filterNot { publicKey =>
      providedSignaturesForKeys.exists(_._1 == publicKey)
    }
  }

}