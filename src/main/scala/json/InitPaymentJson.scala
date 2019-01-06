package json

import core.{ObjectDecoder, ObjectEncoder}
import io.circe.{Decoder, Encoder, Json}
import statements.InitPayment
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder

object InitPaymentJson extends ObjectEncoder[InitPayment] with ObjectDecoder[InitPayment] {
  override lazy val encoder: Encoder[InitPayment] = (statement: InitPayment) => {
    Json.obj(
      ("decoder", "json.InitPaymentJson".asJson),
      ("statement", statement.asJson)
    )
  }

  override lazy val decoder: Decoder[InitPayment] = deriveDecoder[InitPayment]
}
