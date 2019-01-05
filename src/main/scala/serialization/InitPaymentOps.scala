package serialization

import core.{ObjectDecoder, ObjectEncoder}
import io.circe.{Decoder, Encoder, Json}
import statements.InitPayment
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder

object InitPaymentOps extends ObjectEncoder[InitPayment] with ObjectDecoder[InitPayment] {
  override lazy val encoder: Encoder[InitPayment] = (statement: InitPayment) => {
    Json.obj(
      ("decoder", "serialization.InitPaymentOps".asJson),
      ("statement", statement.asJson)
    )
  }

  override lazy val decoder: Decoder[InitPayment] = deriveDecoder[InitPayment]
}
