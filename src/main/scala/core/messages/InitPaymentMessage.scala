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
            keysFileOps: KeysFileOps): Option[InitPaymentMessage] = {
    val timestamp = LocalDateTime.now
    val notSignedMessage = InitPaymentMessage(createdByNode, fromPublicKeyEncoded, toPublicKeyEncoded, money, timestamp)
    val signer = new Signer (keysFileOps)
    keysFileOps.getUserByKey(fromPublicKeyEncoded).map { userName =>
      val signature = signer.sign (userName, fromPublicKeyEncoded, notSignedMessage.dataToSign)
      val encodedSignature = bytesToBase64Str(signature)
      notSignedMessage.copy (encodedSignature = Some(encodedSignature) )
    }
  }

}

case class InitPaymentMessage(val createdByNode: String, val fromPublicKeyEncoded: String, val toPublicKeyEncoded: String, val money: Money,
                              val timestamp: LocalDateTime, val encodedSignature: Option[String] = None) extends Message {

  override def dataToSign: Array[Byte] = (createdByNode + fromPublicKeyEncoded + toPublicKeyEncoded + money + timestamp).getBytes

}