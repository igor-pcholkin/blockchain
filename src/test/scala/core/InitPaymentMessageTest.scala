package core

import io.circe.generic.auto._
import messages.{InitPaymentMessage, Message}
import keys.{KeysFileOps, KeysGenerator, KeysSerializator}
import org.mockito.Matchers
import org.mockito.Mockito.{never, verify, when}
import org.scalatest
import org.scalatest.mockito.MockitoSugar
import org.scalatest.FlatSpec

class InitPaymentMessageTest extends FlatSpec with scalatest.Matchers with MockitoSugar with KeysGenerator {
  "InitPaymentMessage" should "serialize message to json with non empty signature" in {
    val keyPair = generateKeyPair()
    val ks = new KeysSerializator {
      override val keysFileOps: KeysFileOps = mock[KeysFileOps]
    }
    val serializedPublicKey = ks.serialize(keyPair.getPublic)
    when(ks.keysFileOps.getUserByKey("Riga", serializedPublicKey)).thenReturn(Some("Igor"))
    when(ks.keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn(ks.serialize(keyPair.getPrivate))
    when(ks.keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(serializedPublicKey)

    val asset = Money("EUR", 2025)
    val message = InitPaymentMessage("Riga", serializedPublicKey, "5678", asset, ks.keysFileOps).right.get

    message.encodedSignature.getOrElse("").length > 0 shouldBe true
    val jsonMessage = Message.serialize(message)
    println(jsonMessage)
    jsonMessage.startsWith(s"""{"createdByNode":"Riga","fromPublicKeyEncoded":"$serializedPublicKey","toPublicKeyEncoded":"5678","money":{"currency":"EUR","amountInCents":2025},"timestamp":""") shouldBe true
    jsonMessage.contains("\"encodedSignature\":{}") shouldBe false
  }

  "InitPaymentMessage" should "not create payment message if user signing the message is not found for given (from) public key" in {
    val keyPair = generateKeyPair()
    val ks = new KeysSerializator {
      override val keysFileOps: KeysFileOps = mock[KeysFileOps]
    }
    val serializedPublicKey = ks.serialize(keyPair.getPublic)
    when(ks.keysFileOps.getUserByKey("Riga", serializedPublicKey)).thenReturn(None)

    val asset = Money("EUR", 2025)
    InitPaymentMessage("Riga", serializedPublicKey, "5678", asset, ks.keysFileOps ) shouldBe Left("No user with given (from) public key found.")
  }

  "InitPaymentMessage" should "not create payment message if user signing the message is the same as receiver of the payment" in {
    val keyPair = generateKeyPair()
    val ks = new KeysSerializator {
      override val keysFileOps: KeysFileOps = mock[KeysFileOps]
    }
    val serializedPublicKey = ks.serialize(keyPair.getPublic)

    val asset = Money("EUR", 2025)
    InitPaymentMessage("Riga", serializedPublicKey, serializedPublicKey, asset, ks.keysFileOps ) shouldBe Left("Sender and receiver of payment can't be the same person")

    verify(ks.keysFileOps, never).getUserByKey(Matchers.any[String], Matchers.any[String])
  }

}
