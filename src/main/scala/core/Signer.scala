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
  def sign(nodeName: String, userName: String, encodedPublicKey: String, data: Array[Byte]): Array[Byte] = {
    val publicKey = readPublicKey(nodeName, userName)
    if (publicKey != encodedPublicKey) {
      throw new RuntimeException("Specified public key should match that of specified user.")
    }
    val privateKey = deserializePrivate(readPrivateKey(nodeName, userName))
    Signer.sign(privateKey, data)
  }

  def verify(nodeName: String, userName: String, data: Array[Byte], signature: Array[Byte]): Boolean = {
    val publicKey = deserializePublic(readPublicKey(nodeName, userName))
    Signer.verify(signature, data, publicKey)
  }
}
