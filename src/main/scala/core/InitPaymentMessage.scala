package core

import java.time.LocalDateTime

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import keys.KeysFileOps
import util.StringConverter


object InitPaymentMessage extends StringConverter {
  def deserialize(s: String) = decode[InitPaymentMessage](s)

  def apply(createdBy: String, fromPublicKeyEncoded: String, toPublicKeyEncoded: String, money: Money,
            keysFileOps: KeysFileOps): InitPaymentMessage = {
    val timestamp = LocalDateTime.now
    val notSignedMessage = InitPaymentMessage(createdBy, fromPublicKeyEncoded, toPublicKeyEncoded, money, timestamp)
    val signer = new Signer (keysFileOps)
    val signature = signer.sign (createdBy, fromPublicKeyEncoded, notSignedMessage.dataToSign)
    val encodedSignature = bytesToBase64Str(signature)
    notSignedMessage.copy (encodedSignature = Some(encodedSignature) )
  }

}

case class InitPaymentMessage(val createdBy: String, val fromPublicKeyEncoded: String, val toPublicKeyEncoded: String, val money: Money,
                              val timestamp: LocalDateTime, val encodedSignature: Option[String] = None) extends Message {

  def serialize: String = {
    val printer = Printer.noSpaces.copy(dropNullValues = true)
    printer.pretty(this.asJson)
  }

  def dataToSign: Array[Byte] = (createdBy + fromPublicKeyEncoded + toPublicKeyEncoded + money + timestamp).getBytes

}