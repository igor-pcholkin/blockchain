package serialization

import core._
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax._
import serialization.StatementOps._
import io.circe.parser.decode

object FactOps {
  def deserialize(s: String): Either[io.circe.Error, Fact] = {
    decode[Fact](s)
  }

  implicit lazy val encoder: Encoder[Fact] = (fact: Fact) => {
    val statement = fact.statement
    Json.obj(
      ("statement", statement.asJson),
      ("providedSignaturesForKeys", fact.providedSignaturesForKeys.asJson)
    )
  }

  implicit lazy val decoder: Decoder[Fact] = (c: HCursor) => {
    for {
      statement <- c.downField("statement").as[Statement]
      providedSignaturesForKeys <- c.downField("providedSignaturesForKeys").as[Seq[(String, String)]]
    } yield {
      new Fact(statement, providedSignaturesForKeys)
    }
  }

}

