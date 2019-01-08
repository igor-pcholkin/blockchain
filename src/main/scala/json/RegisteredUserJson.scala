package json

import core.{ObjectDecoder, ObjectEncoder}
import io.circe.generic.auto._
import io.circe.generic.semiauto.deriveDecoder
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import statements.RegisteredUser

object RegisteredUserJson extends ObjectEncoder[RegisteredUser] with ObjectDecoder[RegisteredUser] {
  override lazy val encoder: Encoder[RegisteredUser] = (statement: RegisteredUser) => {
    Json.obj(
      ("decoder", "json.RegisteredUserJson".asJson),
      ("statement", statement.asJson)
    )
  }

  override lazy val decoder: Decoder[RegisteredUser] = deriveDecoder[RegisteredUser]
}
