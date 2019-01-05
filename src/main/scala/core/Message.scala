package core

import io.circe
import io.circe.syntax._
import io.circe._
import io.circe.parser.decode
import messages._
import io.circe.Encoder

import scala.reflect.runtime.universe

object Serializator {
  def serialize[T <: Serializable](msg: T)(implicit encoder: Encoder[T]): String = {
    val printer = Printer.noSpaces.copy(dropNullValues = true)
    printer.pretty(msg.asJson)
  }

  def deserialize(s: String): Option[Serializable] = {

    val deserializers = Stream(SignedStatementMessage, NewBlockMessage, AddPeersMessage, RequestBlocksMessage, RequestAllStatementsMessage)

    deserializers.map { d =>
      d.deserialize(s)
    }.find(_.isRight).flatMap {
      case Right(msg) => Some(msg)
      case _ => None
    }
  }

}

/** message is any service information that peers use and exchange each which other and requires serialization */
trait Message extends Serializable {
  def encoder: Encoder[Message]
}

object Message {
  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  lazy val decoder: Decoder[Message] = (c: HCursor) => c.downField("messageType").as[String].flatMap { messageType =>
    val module = runtimeMirror.staticModule(messageType)

    val moduleMirror = runtimeMirror.reflectModule(module)
    implicit val decoder: Decoder[Message] = moduleMirror.instance.asInstanceOf[ObjectDecoder[Message]].getDecoder
    c.downField("message").as
  }
}

/**
  * Message envelope allows to segregate fields which should not be considered a part of message,
  * e.g. sentFromIPAddress - in order not to process the same message several times when it comes from different nodes.
  */
object MessageEnvelope {
  def deserialize(s: String): Either[io.circe.Error, MessageEnvelope] = {
    decode[MessageEnvelope](s)(decoder)
  }

  lazy val encoder: Encoder[MessageEnvelope] = (messageEnvelope: MessageEnvelope) => {
    val message = messageEnvelope.message
    Json.obj(
      ("message", message.asJson(message.encoder)),
      ("sentFromIPAddress", messageEnvelope.sentFromIPAddress.asJson)
    )
  }

  lazy val decoder: Decoder[MessageEnvelope] = (c: HCursor) => for {
    message <- c.downField("message").as[Message](Message.decoder)
    sentFromIPAddress <- c.downField("sentFromIPAddress").as[String]
  } yield {
    new MessageEnvelope(message, sentFromIPAddress)
  }

}

case class MessageEnvelope(message: Message, sentFromIPAddress: String) extends Serializable

trait Deserializator {
  def deserialize(s: String): Either[circe.Error, Serializable]
}
