package serialization

import core.{Message, ObjectDecoder, ObjectEncoder}
import io.circe.{Decoder, Encoder, HCursor}

import scala.reflect.runtime.universe

object MessageOps {
  val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  implicit lazy val messageEncoder = new Encoder[Message] {
    override final def apply(message: Message) = {
      val encoderClassName = s"serialization.${message.getClass.getSimpleName}Ops"
      val concreteEncoder = getInstanceByName(encoderClassName).asInstanceOf[ObjectEncoder[Message]]
      concreteEncoder.getEncoder(message)
    }
  }

  implicit lazy val messageDecoder = new Decoder[Message] {
    override final def apply(c: HCursor) = {

      c.downField("messageType").as[String].flatMap { messageType =>
        implicit val decoder: Decoder[Message] = getInstanceByName(messageType).asInstanceOf[ObjectDecoder[Message]].getDecoder
        c.downField("message").as
      }
    }
  }

  private def getInstanceByName(className: String) = {
    val module = runtimeMirror.staticModule(className)

    runtimeMirror.reflectModule(module).instance
  }

}

