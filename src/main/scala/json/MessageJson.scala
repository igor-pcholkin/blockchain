package json

import core.{Message, ObjectDecoder, ObjectEncoder}
import io.circe.{Decoder, Encoder, HCursor}

import scala.reflect.runtime.universe
import universe.Mirror

object MessageJson {
  val runtimeMirror: Mirror = universe.runtimeMirror(getClass.getClassLoader)

  implicit lazy val messageEncoder: Encoder[Message] = (message: Message) => {
    val encoderClassName = s"json.${message.getClass.getSimpleName}Json"
    val concreteEncoder = getInstanceByName(encoderClassName).asInstanceOf[ObjectEncoder[Message]]
    concreteEncoder.encoder(message)
  }

  implicit lazy val messageDecoder: Decoder[Message] = (c: HCursor) => {
    c.downField("decoder").as[String].flatMap { decoderType =>
      val concreteDecoder = getInstanceByName(decoderType).asInstanceOf[ObjectDecoder[Message]].decoder
      c.downField("message").as(concreteDecoder)
    }
  }

  private def getInstanceByName(className: String) = {
    val module = runtimeMirror.staticModule(className)

    runtimeMirror.reflectModule(module).instance
  }
}

