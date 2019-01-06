package statements

import java.time.LocalDateTime

import business.Money
import core._
import keys.KeysFileOps
import util.StringConverter

object InitPayment extends StringConverter {

  def apply(createdByNode: String, fromPublicKeyEncoded: String, toPublicKeyEncoded: String, money: Money,
            keysFileOps: KeysFileOps, timestamp: LocalDateTime = LocalDateTime.now): Either[String, InitPayment] = {
    if (fromPublicKeyEncoded == toPublicKeyEncoded) {
      Left("Sender and receiver of payment can't be the same person")
    } else {
      Right(InitPayment(createdByNode, fromPublicKeyEncoded, toPublicKeyEncoded, money, timestamp))
    }
  }

}

case class InitPayment(createdByNode: String, fromPublicKeyEncoded: String, toPublicKeyEncoded: String, money: Money,
                       timestamp: LocalDateTime) extends Statement {

  override def dataToSign: Array[Byte] = (createdByNode + fromPublicKeyEncoded + toPublicKeyEncoded + money + timestamp).getBytes

}