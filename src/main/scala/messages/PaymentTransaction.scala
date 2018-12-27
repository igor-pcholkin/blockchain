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
    keysFileOps.getUserByKey(createdByNode, paymentMessage.toPublicKeyEncoded) map { userName =>
      val signer = new Signer(keysFileOps)
      notSignedTransaction.signByUserPublicKey(createdByNode, signer, userName, paymentMessage.toPublicKeyEncoded).asInstanceOf[PaymentTransaction]
    }
  }
}

case class PaymentTransaction(paymentMessage: InitPaymentMessage, timestamp: LocalDateTime) extends Fact with Message {

  override def publicKeysRequiredToSignEncoded: Seq[String] = paymentMessage.publicKeysRequiredToSignEncoded
  override def providedSignaturesForKeys: Seq[(String, String)] = paymentMessage.providedSignaturesForKeys
  override def addSignature(publicKey: String, signature: String) = {
    val updatedPaymentMessage = paymentMessage.addSignature(publicKey, signature)
    copy(paymentMessage = updatedPaymentMessage.asInstanceOf[InitPaymentMessage])
  }

  override def dataToSign: Array[Byte] = (new String(paymentMessage.dataToSign) + timestamp).getBytes
}
