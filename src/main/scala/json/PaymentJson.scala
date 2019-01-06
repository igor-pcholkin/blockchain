package json

import core.{ObjectDecoder, ObjectEncoder}
import io.circe.{Decoder, Encoder, Json}
import statements.Payment
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder

object PaymentJson extends ObjectEncoder[Payment] with ObjectDecoder[Payment] {
  override lazy val encoder: Encoder[Payment] = (statement: Payment) => {
    Json.obj(
      ("decoder", "json.PaymentJson".asJson),
      ("statement", statement.asJson)
    )
  }

  override lazy val decoder: Decoder[Payment] = deriveDecoder[Payment]
}
