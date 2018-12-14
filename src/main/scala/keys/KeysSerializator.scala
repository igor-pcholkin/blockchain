package keys

import java.io.{File, FileInputStream}
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}

import scala.io.Source

trait KeysSerializator {
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
    val fileName = s"$userName/privateKey"
    val fis = new FileInputStream(fileName)
    val size = new File(fileName).length()
    val buffer = Array.ofDim[Byte](size.toInt)
    fis.read(buffer)
    val key = deserializePrivate(buffer)
    fis.close()
    key
  }

  def readPublicKey(userName: String) = {
    val fileName = s"$userName/publicKey"
    val fis = new FileInputStream(fileName)
    val size = new File(fileName).length()
    val buffer = Array.ofDim[Byte](size.toInt)
    fis.read(buffer)
    val key = deserializePublic(buffer)
    fis.close()
    key
  }

}
