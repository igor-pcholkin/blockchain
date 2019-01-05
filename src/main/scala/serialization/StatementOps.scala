package serialization

import core.{ObjectDecoder, ObjectEncoder, Statement}
import io.circe.{Decoder, Encoder, HCursor}

import scala.reflect.runtime.universe

object StatementOps {
  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  implicit lazy val statementEncoder = new Encoder[Statement] {
    override final def apply(statement: Statement) = {
      val encoderClassName = s"serialization.${statement.getClass.getSimpleName}Ops"
      val concreteEncoder = getInstanceByName(encoderClassName).asInstanceOf[ObjectEncoder[Statement]]
      concreteEncoder.encoder(statement)
    }
  }

  implicit lazy val statementDecoder: Decoder[Statement] = (c: HCursor) => c.downField("decoder").as[String].flatMap { decoderType =>
    val concreteDecoder = getInstanceByName(decoderType).asInstanceOf[ObjectDecoder[Statement]].decoder
    c.downField("statement").as(concreteDecoder)
  }

  private def getInstanceByName(className: String) = {
    val module = runtimeMirror.staticModule(className)

    runtimeMirror.reflectModule(module).instance
  }

}
