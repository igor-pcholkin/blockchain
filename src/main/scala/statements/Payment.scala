package statements

import java.time.LocalDateTime

import business.Money
import core._
import util.StringConverter

object Payment extends StringConverter {

  def verifyAndCreate(createdByNode: String, fromPublicKeyEncoded: String, toPublicKeyEncoded: String, money: Money,
            timestamp: LocalDateTime = LocalDateTime.now): Either[String, Payment] = {
    if (fromPublicKeyEncoded == toPublicKeyEncoded) {
      Left("Sender and receiver of payment can't be the same person")
    } else {
      Right(Payment(createdByNode, fromPublicKeyEncoded, toPublicKeyEncoded, money, timestamp))
    }
  }

}

case class Payment(createdByNode: String, fromPublicKeyEncoded: String, toPublicKeyEncoded: String, money: Money,
                   timestamp: LocalDateTime) extends Statement {

  override def dataToSign: Array[Byte] = (createdByNode + fromPublicKeyEncoded + toPublicKeyEncoded + money + timestamp).getBytes

}