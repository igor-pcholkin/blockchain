package messages

import java.time.LocalDateTime

import core._
import io.circe
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe._
import io.circe.syntax._
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
      Right(InitPaymentMessage(createdByNode, fromPublicKeyEncoded, toPublicKeyEncoded, money, timestamp))
    }
  }

}

case class InitPaymentMessage(createdByNode: String, fromPublicKeyEncoded: String, toPublicKeyEncoded: String, money: Money,
    timestamp: LocalDateTime) extends Statement {

  override def dataToSign: Array[Byte] = (createdByNode + fromPublicKeyEncoded + toPublicKeyEncoded + money + timestamp).getBytes

  lazy val encoder: Encoder[Statement] = new Encoder[InitPaymentMessage] {
    final def apply(message: InitPaymentMessage): Json = {
      Json.obj(
        ("statementType", "InitPaymentMessage".asJson),
        ("statement", message.asJson)
      )
    }
  }.asInstanceOf[Encoder[Statement]]

}