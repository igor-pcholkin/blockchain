package core

import io.circe
import io.circe.Encoder
import keys.KeysFileOps
import io.circe._
import io.circe.syntax._
import messages.InitPaymentMessage
import io.circe.generic.auto._
import io.circe.parser.decode
import util.StringConverter

/**
  * Statement is any business specific data which requires agreement (signing) between peers
  * As opposed to facts, statements are not stored in blockchain.
  * */
trait Statement extends Message {
  def dataToSign: Array[Byte]

  def encoder: Encoder[Statement]
}

object Statement {
  lazy val decoder: Decoder[Statement] = (c: HCursor) => c.downField("statementType").as[String].flatMap { statementType =>
    if (statementType == "InitPaymentMessage") {
      c.downField("statement").as[InitPaymentMessage]
    } else {
      throw new RuntimeException("Unkonown statement type")
    }
  }
}

object SignedStatement extends MsgDeserializator {
  def deserialize(s: String): Either[circe.Error, SignedStatement] = decode[SignedStatement](s)(decoder)

  def apply(statement: Statement, publicKeysRequiredToSignEncoded: Seq[String],
            nodeName: String, keysFileOps: KeysFileOps): SignedStatement = {
    val signedStatement = SignedStatement(statement, publicKeysRequiredToSignEncoded)
    val newSignatures = signedStatement.signByLocalPublicKeys(nodeName, keysFileOps)
    signedStatement.addSignatures(newSignatures)
  }

  lazy val encoder: Encoder[SignedStatement] = (signedStatement: SignedStatement) => {
    val statement = signedStatement.statement
    Json.obj(
      ("statement", statement.asJson(statement.encoder)),
      ("publicKeysRequiredToSignEncoded", signedStatement.publicKeysRequiredToSignEncoded.asJson),
      ("providedSignaturesForKeys", signedStatement.providedSignaturesForKeys.asJson)
    )
  }

  lazy val decoder: Decoder[SignedStatement] = (c: HCursor) => for {
    statement <- c.downField("statement").as[Statement](Statement.decoder)
    publicKeysRequiredToSignEncoded <- c.downField("publicKeysRequiredToSignEncoded").as[Seq[String]]
    providedSignaturesForKeys <- c.downField("providedSignaturesForKeys").as[Seq[(String, String)]]
  } yield {
    new SignedStatement(statement, publicKeysRequiredToSignEncoded, providedSignaturesForKeys)
  }

}

/**
  * SignedStatement is a wrapper which attaches signatures to statements.
  * As opposed to facts, statements are not stored in blockchain.
  */
case class SignedStatement(statement: Statement, publicKeysRequiredToSignEncoded: Seq[String], providedSignaturesForKeys: Seq[(String, String)] = Nil)
  extends Message with StringConverter {

  def addSignature(publicKey: String, signature: String): SignedStatement = {
    copy(providedSignaturesForKeys = providedSignaturesForKeys :+ (publicKey, signature))
  }

  def addSignatures(signatures: Seq[(String, String)]): SignedStatement = {
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