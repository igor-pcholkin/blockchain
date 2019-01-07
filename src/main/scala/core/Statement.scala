package core

import java.time.LocalDateTime

import io.circe.Encoder
import io.circe._

/**
  * Statement is any business specific data which potentially requires agreement (signing) between peers.
  * As opposed to facts, statements are not stored in blockchain.
  * */
trait Statement {
  val timestamp: LocalDateTime

  def dataToSign: Array[Byte]
}

trait ObjectDecoder[T] {
  def decoder: Decoder[T]
}

trait ObjectEncoder[T] {
  def encoder: Encoder[T]
}