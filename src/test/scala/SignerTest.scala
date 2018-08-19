import core.{PrivateKey, PublicKey, Signer}
import org.scalatest._

class SignerTest extends FlatSpec with Matchers {
  "Signer" should "sign data with a private key" in {
    val privateKey = PrivateKey()
    val publicKey = PublicKey()
    val data = "123"
    val signature  = Signer.sign(privateKey, data)
    Signer.verify(signature, publicKey) shouldBe true
  }
}
