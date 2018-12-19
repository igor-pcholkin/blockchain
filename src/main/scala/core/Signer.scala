package core

import java.security.{PrivateKey, PublicKey, Signature}

import keys.{KeysFileOps, KeysSerializator}

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

class Signer(val keysFileOps: KeysFileOps) extends KeysSerializator {
  def sign(userName: String, encodedPublicKey: String, initPayment: InitPaymentMessage): Array[Byte] = {
    val publicKey = readPublicKey(userName)
    if (publicKey != deserializePublic(encodedPublicKey)) {
      throw new RuntimeException("Specified public key should match that of specified user.")
    }
    val privateKey = readPrivateKey(userName)
    Signer.sign(privateKey, initPayment.dataToSign)
  }

  def verify(userName: String, initPayment: InitPaymentMessage, signature: Array[Byte]): Boolean = {
    val publicKey = readPublicKey(userName)
    Signer.verify(signature, initPayment.dataToSign, publicKey)
  }
}
