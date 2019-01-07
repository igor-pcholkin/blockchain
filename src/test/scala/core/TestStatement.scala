package core
import java.time.LocalDateTime

case class TestStatement(data: String, override val timestamp: LocalDateTime = LocalDateTime.now()) extends Statement {

  override def dataToSign: Array[Byte] = data.getBytes

}

