package statements

import java.time.LocalDateTime

import core._
import io.circe
import io.circe.{Encoder, _}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.decode
import io.circe.syntax._
import keys.KeysFileOps
import util.StringConverter


object InitPayment extends StringConverter with Deserializator with StatementDecoder {
  override def deserialize(s: String): Either[circe.Error, InitPayment] = decode[InitPayment](s)

  override def getDecoder: Decoder[Statement] = deriveDecoder[InitPayment].asInstanceOf[Decoder[Statement]]

  def apply(createdByNode: String, fromPublicKeyEncoded: String, toPublicKeyEncoded: String, money: Money,
            keysFileOps: KeysFileOps): Either[String, InitPayment] = {
    if (fromPublicKeyEncoded == toPublicKeyEncoded) {
      Left("Sender and receiver of payment can't be the same person")
    } else {
      val timestamp = LocalDateTime.now
      Right(InitPayment(createdByNode, fromPublicKeyEncoded, toPublicKeyEncoded, money, timestamp))
    }
  }

}

case class InitPayment(createdByNode: String, fromPublicKeyEncoded: String, toPublicKeyEncoded: String, money: Money,
                       timestamp: LocalDateTime) extends Statement {

  override def dataToSign: Array[Byte] = (createdByNode + fromPublicKeyEncoded + toPublicKeyEncoded + money + timestamp).getBytes

  lazy val encoder: Encoder[Statement] = new Encoder[InitPayment] {
    final def apply(message: InitPayment): Json = {
      Json.obj(
        ("statementType", "statements.InitPayment".asJson),
        ("statement", message.asJson)
      )
    }
  }.asInstanceOf[Encoder[Statement]]

}