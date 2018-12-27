package core

abstract class Statement {
  def publicKeysRequiredToSignEncoded: Seq[String]
  def providedSignaturesForKeys: Seq[(String, String)]

  def dataToSign: Array[Byte] = Array[Byte]()
}

abstract class Fact extends Statement