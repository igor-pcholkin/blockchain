package core

import io.circe.Encoder
import io.circe.generic.semiauto._

case class TestStatement(data: String) extends Statement {

  override def dataToSign: Array[Byte] = data.getBytes

  override def encoder: Encoder[Statement] = deriveEncoder[TestStatement].asInstanceOf[Encoder[Statement]]

}

