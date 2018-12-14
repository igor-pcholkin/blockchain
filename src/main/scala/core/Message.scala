package core

import java.time.LocalDateTime

abstract class Message {

}

case class Invoice(createdBy: String, from: String, to: String, asset: Asset, timestamp: LocalDateTime) extends Message