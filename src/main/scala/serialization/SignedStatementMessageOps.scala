package serialization

import core.{ObjectDecoder, ObjectEncoder, Statement}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}
import messages.SignedStatementMessage
import serialization.StatementOps._

object SignedStatementMessageOps extends ObjectEncoder[SignedStatementMessage] with ObjectDecoder[SignedStatementMessage] {
  override lazy val encoder: Encoder[SignedStatementMessage] = (message: SignedStatementMessage) => {
    val statement = message.statement
    Json.obj (
      ("decoder", "serialization.SignedStatementMessageOps".asJson),
      ("message", Json.obj(

        ("statement", statement.asJson),
        ("publicKeysRequiredToSignEncoded", message.publicKeysRequiredToSignEncoded.asJson),
        ("providedSignaturesForKeys", message.providedSignaturesForKeys.asJson)
      ))
    )
  }

  override lazy val decoder: Decoder[SignedStatementMessage] = (c: HCursor) => for {
    statement <- c.downField("statement").as[Statement]
    publicKeysRequiredToSignEncoded <- c.downField("publicKeysRequiredToSignEncoded").as[Seq[String]]
    providedSignaturesForKeys <- c.downField("providedSignaturesForKeys").as[Seq[(String, String)]]
  } yield {
    new SignedStatementMessage(statement, publicKeysRequiredToSignEncoded, providedSignaturesForKeys)
  }

}
