package messages

import java.time.LocalDateTime

import core.{Fact, Message, Signer}
import io.circe
import io.circe.generic.auto._
import io.circe.parser.decode
import keys.KeysFileOps
import util.StringConverter

object PaymentTransaction extends StringConverter {
  def deserialize(s: String): Either[circe.Error, PaymentTransaction] = decode[PaymentTransaction](s)

  def apply(createdByNode: String, paymentMessage: InitPaymentMessage, keysFileOps: KeysFileOps): Option[PaymentTransaction] = {
    val timestamp = LocalDateTime.now
    val notSignedTransaction = PaymentTransaction(paymentMessage, timestamp)
    val signer = new Signer(keysFileOps)
    keysFileOps.getUserByKey(createdByNode, paymentMessage.toPublicKeyEncoded) map { userName =>
      val signature = signer.sign(createdByNode, userName, paymentMessage.toPublicKeyEncoded, notSignedTransaction.dataToSign)
      val encodedSignature = bytesToBase64Str(signature)
      notSignedTransaction.copy(encodedSignature = Some(encodedSignature))
    }
  }
}

case class PaymentTransaction(paymentMessage: InitPaymentMessage, timestamp: LocalDateTime, encodedSignature: Option[String] = None) extends Fact with Message {

  override def publicKeysRequiredToSignEncoded: Seq[String] = ??? // TBD
  override def providedSignaturesForKeys: Seq[(String, String)] = ???

  override def dataToSign: Array[Byte] = (paymentMessage.toString + timestamp).getBytes
}
