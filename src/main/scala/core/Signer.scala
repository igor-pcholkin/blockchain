package core

import java.security.{PrivateKey, PublicKey, Signature}

object Signer {
  val algorithm = "SHA1withECDSA"

  def sign(privateKey: PrivateKey, data: Array[Byte]): Array[Byte] = {
    val javaSecSignature = Signature.getInstance(algorithm)
    javaSecSignature.initSign(privateKey)
    javaSecSignature.update(data)

    javaSecSignature.sign
  }

  def verify(signature: Array[Byte], originalData: Array[Byte], publicKey: PublicKey): Boolean = {
    val javaSecSignature = Signature.getInstance(algorithm)
    javaSecSignature.initVerify(publicKey)
    javaSecSignature.update(originalData)
    javaSecSignature.verify(signature)
  }
}
