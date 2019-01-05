package core

case class TestStatement(data: String) extends Statement {

  override def dataToSign: Array[Byte] = data.getBytes

}

