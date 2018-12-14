package core

import java.security.{PrivateKey, PublicKey, Signature}

import keys.KeysSerializator

object Signer extends KeysSerializator {
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

  def sign(userName: String, initPayment: InitPayment): Array[Byte] = {
    val privateKey = readPrivateKey(userName)
    sign(privateKey, initPayment.dataToSign)
  }

  def verify(userName: String, initPayment: InitPayment, signature: Array[Byte]): Boolean = {
    val publicKey = readPublicKey(userName)
    verify(signature, initPayment.dataToSign, publicKey)
  }
}
