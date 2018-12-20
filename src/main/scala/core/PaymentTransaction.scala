package core

import java.time.LocalDateTime
import io.circe.Printer
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

import keys.KeysFileOps
import util.StringConverter

object PaymentTransaction extends StringConverter {
  def deserialize(s: String) = decode[PaymentTransaction](s)

  def apply(createdBy: String, paymentMessage: InitPaymentMessage, keysFileOps: KeysFileOps): PaymentTransaction = {
    val timestamp = LocalDateTime.now
    val notSignedTransaction = PaymentTransaction(paymentMessage, timestamp)
    val signer = new Signer(keysFileOps)
    val signature = signer.sign(createdBy, paymentMessage.toPublicKeyEncoded, notSignedTransaction.dataToSign)
    val encodedSignature = bytesToBase64Str(signature)
    notSignedTransaction.copy(encodedSignature = Some(encodedSignature))
  }
}

case class PaymentTransaction(paymentMessage: InitPaymentMessage, timestamp: LocalDateTime, encodedSignature: Option[String] = None) {

  def serialize: String = {
    val printer = Printer.noSpaces.copy(dropNullValues = true)
    printer.pretty(this.asJson)
  }

  def dataToSign: Array[Byte] = (paymentMessage.toString + timestamp).getBytes
}
