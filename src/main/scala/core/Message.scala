package core

import java.time.LocalDateTime

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.apache.commons.codec.binary.Base64

abstract class Message {
  def serialize: String
}

object InitPaymentMessage {
  def deserialize(s: String) =
    decode[InitPaymentMessage](s) map { msg =>
      val signatureDecoded = Base64.decodeBase64(msg.encodedSignature.getOrElse("").getBytes)
      msg.copy(signature = Some(signatureDecoded), encodedSignature = None)
    }
}

case class InitPaymentMessage(createdBy: String, from: String, to: String, money: Money, timestamp: LocalDateTime,
                              signature: Option[Array[Byte]] = None, encodedSignature: Option[String] = None) extends Message {
  def dataToSign = (createdBy + from + to + money + timestamp).getBytes

  def serialize: String = {
    val base64SignEncoded = new String(Base64.encodeBase64(signature.getOrElse(Array[Byte]())))
    val printer = Printer.noSpaces.copy(dropNullValues = true)
    val msgForJson = this.copy(signature = None, encodedSignature = Some(base64SignEncoded))
    printer.pretty(msgForJson.asJson)
  }

}