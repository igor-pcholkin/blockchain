package core

import java.time.LocalDateTime

abstract class Message

case class InitPayment(createdBy: String, from: String, to: String, asset: Asset, timestamp: LocalDateTime,
                       fromSignature: Option[Array[Byte]] = None) extends Message {
  def dataToSign = (createdBy + from + to + asset + timestamp).getBytes
}