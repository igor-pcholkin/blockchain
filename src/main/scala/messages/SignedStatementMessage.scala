package messages

import core._
import io.circe
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax._
import io.circe.parser.decode
import keys.KeysFileOps
import util.StringConverter

object SignedStatementMessage extends Deserializator with ObjectDecoder[Message] {
  def deserialize(s: String): Either[circe.Error, SignedStatementMessage] = decode[SignedStatementMessage](s)(decoder)

  override def getDecoder: Decoder[Message] = decoder.asInstanceOf[Decoder[Message]]

  def apply(statement: Statement, publicKeysRequiredToSignEncoded: Seq[String],
            nodeName: String, keysFileOps: KeysFileOps): SignedStatementMessage = {
    val signedStatement = new SignedStatementMessage(statement, publicKeysRequiredToSignEncoded)
    val newSignatures = signedStatement.signByLocalPublicKeys(nodeName, keysFileOps)
    signedStatement.addSignatures(newSignatures)
  }

  lazy val decoder: Decoder[SignedStatementMessage] = (c: HCursor) => for {
    statement <- c.downField("statement").as[Statement](Statement.decoder)
    publicKeysRequiredToSignEncoded <- c.downField("publicKeysRequiredToSignEncoded").as[Seq[String]]
    providedSignaturesForKeys <- c.downField("providedSignaturesForKeys").as[Seq[(String, String)]]
  } yield {
    new SignedStatementMessage(statement, publicKeysRequiredToSignEncoded, providedSignaturesForKeys)
  }

  lazy val encoder: Encoder[SignedStatementMessage] = (signedStatement: SignedStatementMessage) => {
    val statement = signedStatement.statement
    Json.obj (
      ("messageType", "messages.SignedStatementMessage".asJson),
      ("message", Json.obj(

        ("statement", statement.asJson(statement.encoder)),
        ("publicKeysRequiredToSignEncoded", signedStatement.publicKeysRequiredToSignEncoded.asJson),
        ("providedSignaturesForKeys", signedStatement.providedSignaturesForKeys.asJson)
      ))
    )
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

  override lazy val encoder: Encoder[Message] = SignedStatementMessage.encoder.asInstanceOf[Encoder[Message]]
}