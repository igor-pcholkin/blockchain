package core

case class PublicKey()
case class PrivateKey()
case class Signature()

object Signer {
  def sign(privateKey: PrivateKey, data: String): Signature = {
    Signature()
  }

  def verify(signature: Signature, publicKey: PublicKey): Boolean = {
    true
  }
}
