package messages

import java.time.LocalDateTime

import core._
import io.circe
import io.circe.generic.auto._
import io.circe.parser.decode
import keys.KeysFileOps
import util.StringConverter


object InitPaymentMessage extends StringConverter with MsgDeserializator {
  override def deserialize(s: String): Either[circe.Error, InitPaymentMessage] = decode[InitPaymentMessage](s)

  def apply(createdByNode: String, fromPublicKeyEncoded: String, toPublicKeyEncoded: String, money: Money,
            keysFileOps: KeysFileOps): Either[String, InitPaymentMessage] = {
    if (fromPublicKeyEncoded == toPublicKeyEncoded) {
      Left("Sender and receiver of payment can't be the same person")
    } else {
      val timestamp = LocalDateTime.now
      val notSignedMessage = InitPaymentMessage(createdByNode, fromPublicKeyEncoded, toPublicKeyEncoded, money, timestamp)
      keysFileOps.getUserByKey(createdByNode, fromPublicKeyEncoded) match {
        case Some(userName) =>
          val signer = new Signer(keysFileOps)
          Right(notSignedMessage.signByUserPublicKey(createdByNode, signer, userName, fromPublicKeyEncoded).asInstanceOf[InitPaymentMessage])
        case None =>
          Left("No user with given (from) public key found.")
      }
    }
  }

}

case class InitPaymentMessage(createdByNode: String, fromPublicKeyEncoded: String, toPublicKeyEncoded: String, money: Money,
    timestamp: LocalDateTime, override val providedSignaturesForKeys: Seq[(String, String)] = Nil)
      extends Statement with Message {

  override val publicKeysRequiredToSignEncoded: Seq[String] = Seq(fromPublicKeyEncoded, toPublicKeyEncoded)

  override def addSignature(publicKey: String, signature: String): Statement = {
    copy(providedSignaturesForKeys = providedSignaturesForKeys :+ (publicKey, signature))
  }

  override def dataToSign: Array[Byte] = (createdByNode + fromPublicKeyEncoded + toPublicKeyEncoded + money + timestamp).getBytes

}