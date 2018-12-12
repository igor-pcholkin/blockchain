package keys

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}

object Serialization {
  def serialize(privateKey: PrivateKey): Array[Byte] = privateKey.getEncoded
  def serialize(publicKey: PublicKey): Array[Byte] = publicKey.getEncoded

  def deserializePrivate(bytes: Array[Byte]): PrivateKey = {
    val ecKeyFac = KeyFactory.getInstance("EC")

    val privateKeySpec = new PKCS8EncodedKeySpec(bytes)
    ecKeyFac.generatePrivate(privateKeySpec)
  }

  def deserializePublic(bytes: Array[Byte]): PublicKey = {
    val ecKeyFac = KeyFactory.getInstance("EC")
    val x509EncodedKeySpec = new X509EncodedKeySpec(bytes)
    ecKeyFac.generatePublic(x509EncodedKeySpec)
  }
}
