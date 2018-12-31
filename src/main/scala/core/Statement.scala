package core

import io.circe.Encoder
import io.circe._
import scala.reflect.runtime.universe

/**
  * Statement is any business specific data which potentially requires agreement (signing) between peers.
  * As opposed to facts, statements are not stored in blockchain.
  * */
trait Statement extends Serializable {
  def dataToSign: Array[Byte]

  def encoder: Encoder[Statement]
}

object Statement {
  private val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  lazy val decoder: Decoder[Statement] = (c: HCursor) => c.downField("statementType").as[String].flatMap { statementType =>
    val module = runtimeMirror.staticModule(statementType)

    val moduleMirror = runtimeMirror.reflectModule(module)
    implicit val decoder: Decoder[Statement] = moduleMirror.instance.asInstanceOf[StatementDecoder].getDecoder
    c.downField("statement").as
  }
}

trait StatementDecoder {
  def getDecoder: Decoder[Statement]
}