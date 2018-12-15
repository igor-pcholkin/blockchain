package keys

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}

trait KeysSerializator {
  val keysFileOps: KeysFileOps

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

  def readPrivateKey(userName: String) = {
    val buffer = keysFileOps.readKeyFromFile(s"$userName/privateKey")
    deserializePrivate(buffer)
  }

  def readPublicKey(userName: String) = {
    val buffer = keysFileOps.readKeyFromFile(s"$userName/publicKey")
    deserializePublic(buffer)
  }

  def writeKey(userName: String, privateKey: PrivateKey) = {
    keysFileOps.writeKey(s"$userName/privateKey", serialize(privateKey))
  }

  def writeKey(userName: String, publicKey: PublicKey) = {
    keysFileOps.writeKey(s"$userName/publicKey", serialize(publicKey))
  }
}
