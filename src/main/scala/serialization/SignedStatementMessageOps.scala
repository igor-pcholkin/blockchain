package serialization

import core.ObjectEncoder
import io.circe.syntax._
import io.circe.{Encoder, Json}
import messages.SignedStatementMessage

object SignedStatementMessageOps extends ObjectEncoder[SignedStatementMessage] {
  override def getEncoder: Encoder[SignedStatementMessage] = new Encoder[SignedStatementMessage] {
    final def apply(message: SignedStatementMessage): Json = {
      val statement = message.statement
      Json.obj (
        ("messageType", "messages.SignedStatementMessage".asJson),
        ("message", Json.obj(

          ("statement", statement.asJson(statement.encoder)),
          ("publicKeysRequiredToSignEncoded", message.publicKeysRequiredToSignEncoded.asJson),
          ("providedSignaturesForKeys", message.providedSignaturesForKeys.asJson)
        ))
      )
    }
  }
}
