package core

import io.circe
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.parser.decode
import io.circe.syntax._

object Fact extends MsgDeserializator {
  override def deserialize(s: String): Either[circe.Error, Fact] = decode[Fact](s)(decoder)

  lazy val encoder: Encoder[Fact] = (fact: Fact) => {
      val statement = fact.statement
      Json.obj(
        ("statement", statement.asJson(statement.encoder)),
        ("providedSignaturesForKeys", fact.providedSignaturesForKeys.asJson)
      )
  }

  lazy val decoder: Decoder[Fact] = (c: HCursor) => {
    for {
      statement <- c.downField("statement").as[Statement](Statement.decoder)
      providedSignaturesForKeys <- c.downField("providedSignaturesForKeys").as[Seq[(String, String)]]
    } yield {
      new Fact(statement, providedSignaturesForKeys)
    }
  }
}

/**
  * Fact is a statement signed by all users which are required to sign it.
  * Facts are stored in blockchain.
  */
case class Fact(statement: Statement, providedSignaturesForKeys: Seq[(String, String)]) extends Message