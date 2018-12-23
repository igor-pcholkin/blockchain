package keys

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}

import util.StringConverter

trait KeysSerializator extends StringConverter {
  val keysFileOps: KeysFileOps

  def serialize(privateKey: PrivateKey): String = bytesToBase64Str(privateKey.getEncoded)
  def serialize(publicKey: PublicKey): String = bytesToBase64Str(publicKey.getEncoded)

  def deserializePrivate(key: String): PrivateKey = {
    val ecKeyFac = KeyFactory.getInstance("EC")
    val decodedKey = base64StrToBytes(key)
    val privateKeySpec = new PKCS8EncodedKeySpec(decodedKey)
    ecKeyFac.generatePrivate(privateKeySpec)
  }

  def deserializePublic(key: String): PublicKey = {
    val ecKeyFac = KeyFactory.getInstance("EC")
    val decodedKey = base64StrToBytes(key)
    val x509EncodedKeySpec = new X509EncodedKeySpec(decodedKey)
    ecKeyFac.generatePublic(x509EncodedKeySpec)
  }

  def readPrivateKey(nodeName: String, userName: String): PrivateKey = {
    val buffer = keysFileOps.readKeyFromFile(nodeName, userName, "privateKey")
    deserializePrivate(buffer)
  }

  def readPublicKey(nodeName: String, userName: String): PublicKey = {
    val buffer = keysFileOps.readKeyFromFile(nodeName, userName, "publicKey")
    deserializePublic(buffer)
  }

  def writeKey(nodeName: String, userName: String, privateKey: PrivateKey): Unit = {
    keysFileOps.writeKeyToFile(nodeName, userName, "privateKey", serialize(privateKey))
  }

  def writeKey(nodeName: String, userName: String, publicKey: PublicKey): Unit = {
    keysFileOps.writeKeyToFile(nodeName, userName, "publicKey", serialize(publicKey))
  }
}
