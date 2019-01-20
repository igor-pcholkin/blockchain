package json

import core.{ObjectDecoder, ObjectEncoder}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import statements.ApprovedFact

object ApprovedFactJson extends ObjectEncoder[ApprovedFact] with ObjectDecoder[ApprovedFact] {
  override lazy val encoder: Encoder[ApprovedFact] = (statement: ApprovedFact) => {
    Json.obj(
      ("decoder", "json.ApprovedFactJson".asJson),
      ("statement", statement.asJson)
    )
  }

  override lazy val decoder: Decoder[ApprovedFact] = deriveDecoder[ApprovedFact]
}
