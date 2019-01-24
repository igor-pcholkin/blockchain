package core

import keys.KeysSerializator
import util.StringConverter

abstract class SignedStatementLike extends StringConverter {
  val statement: Statement
  val providedSignaturesForKeys: Seq[(String, String)]

  def verifySignatures(keysSerializator: KeysSerializator): Boolean = {
    providedSignaturesForKeys.forall {
      case (encodedPublicKey, signature) => verifySignature(encodedPublicKey, signature, keysSerializator)
    }
  }

  private def verifySignature(publicKeyEncoded: String,
                              encodedSignature: String, keysSerializator: KeysSerializator): Boolean = {
    val decodedSignature = base64StrToBytes(encodedSignature)
    val decodedPublicKey = keysSerializator.deserializePublic(publicKeyEncoded)
    Signer.verify(decodedSignature, statement.dataToSign, decodedPublicKey)
  }
}

/**
  * Fact is a statement signed by all users which are required to sign it.
  * Facts are stored in blockchain.
  */
case class Fact(override val statement: Statement, override val providedSignaturesForKeys: Seq[(String, String)])
  extends SignedStatementLike