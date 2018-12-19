package keys

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}

import org.apache.commons.codec.binary.Base64

trait KeysSerializator {
  val keysFileOps: KeysFileOps

  def serialize(privateKey: PrivateKey): String = new String(Base64.encodeBase64(privateKey.getEncoded))
  def serialize(publicKey: PublicKey): String = new String(Base64.encodeBase64(publicKey.getEncoded))

  def deserializePrivate(key: String): PrivateKey = {
    val ecKeyFac = KeyFactory.getInstance("EC")
    val decodedKey = Base64.decodeBase64(key)
    val privateKeySpec = new PKCS8EncodedKeySpec(decodedKey)
    ecKeyFac.generatePrivate(privateKeySpec)
  }

  def deserializePublic(key: String): PublicKey = {
    val ecKeyFac = KeyFactory.getInstance("EC")
    val decodedKey = Base64.decodeBase64(key)
    val x509EncodedKeySpec = new X509EncodedKeySpec(decodedKey)
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
