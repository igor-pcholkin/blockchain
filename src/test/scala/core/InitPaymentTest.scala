package core

import io.circe.generic.auto._
import keys.{KeysFileOps, KeysGenerator, KeysSerializator}
import org.mockito.Matchers
import org.mockito.Mockito.{never, verify, when}
import org.scalatest
import org.scalatest.mockito.MockitoSugar
import org.scalatest.FlatSpec
import serialization.Serializator
import statements.InitPayment

class InitPaymentTest extends FlatSpec with scalatest.Matchers with MockitoSugar with KeysGenerator {
  "InitPaymentMessage" should "serialize message to json with non empty signature" in {
    val keyPair = generateKeyPair()
    val ks = new KeysSerializator {
      override val keysFileOps: KeysFileOps = mock[KeysFileOps]
    }
    val fromPublicKey = ks.serialize(keyPair.getPublic)
    val toPublicKey = "5678"
    when(ks.keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    when(ks.keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn(ks.serialize(keyPair.getPrivate))
    when(ks.keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)

    val asset = Money("EUR", 2025)
    val message = InitPayment("Riga", fromPublicKey, toPublicKey, asset, ks.keysFileOps).right.get

    val jsonMessage = Serializator.serialize(message)
    println(jsonMessage)
    jsonMessage.startsWith(s"""{"createdByNode":"Riga","fromPublicKeyEncoded":"$fromPublicKey","toPublicKeyEncoded":"$toPublicKey","money":{"currency":"EUR","amountInCents":2025},"timestamp":""") shouldBe true
    jsonMessage.contains("\"encodedSignature\":{}") shouldBe false
  }

  "InitPaymentMessage" should "not create payment message if user signing the message is the same as receiver of the payment" in {
    val keyPair = generateKeyPair()
    val ks = new KeysSerializator {
      override val keysFileOps: KeysFileOps = mock[KeysFileOps]
    }
    val serializedPublicKey = ks.serialize(keyPair.getPublic)

    val asset = Money("EUR", 2025)
    InitPayment("Riga", serializedPublicKey, serializedPublicKey, asset, ks.keysFileOps) shouldBe Left("Sender and receiver of payment can't be the same person")

    verify(ks.keysFileOps, never).getUserByKey(Matchers.any[String], Matchers.any[String])
  }

}
