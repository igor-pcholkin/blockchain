package core.messages

import java.time.LocalDateTime

import core.{Money, Signer}
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
      val signer = new Signer(keysFileOps)
      keysFileOps.getUserByKey(createdByNode, fromPublicKeyEncoded) match {
        case Some(userName) =>
          val signature = signer.sign(createdByNode, userName, fromPublicKeyEncoded, notSignedMessage.dataToSign)
          val encodedSignature = bytesToBase64Str(signature)
          Right(notSignedMessage.copy(encodedSignature = Some(encodedSignature)))
        case None =>
          Left("No user with given (from) public key found.")
      }
    }
  }

}

case class InitPaymentMessage(createdByNode: String, fromPublicKeyEncoded: String, toPublicKeyEncoded: String, money: Money,
                              timestamp: LocalDateTime, encodedSignature: Option[String] = None) extends Message {

  override def dataToSign: Array[Byte] = (createdByNode + fromPublicKeyEncoded + toPublicKeyEncoded + money + timestamp).getBytes

}