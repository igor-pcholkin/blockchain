package core

import io.circe.generic.auto._

import core.messages.{InitPaymentMessage, Message}
import keys.{KeysFileOps, KeysGenerator, KeysSerializator}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class InitPaymentMessageTest extends FlatSpec with Matchers with MockitoSugar with KeysGenerator with KeysSerializator {
  val keysFileOps = mock[KeysFileOps]

  "InitPaymentMessage" should "serialize message to json with non empty signature" in {
    val keyPair = generateKeyPair()
    val serializedPublicKey = serialize(keyPair.getPublic)
    when(keysFileOps.getUserByKey(serializedPublicKey)).thenReturn(Some("Igor"))
    when(keysFileOps.readKeyFromFile("keys/Igor/privateKey")).thenReturn(serialize(keyPair.getPrivate))
    when(keysFileOps.readKeyFromFile("keys/Igor/publicKey")).thenReturn(serializedPublicKey)

    val asset = Money("EUR", 2025)
    val message = InitPaymentMessage("Riga", serializedPublicKey, "5678", asset, keysFileOps ).get

    message.encodedSignature.getOrElse("").length > 0 shouldBe true
    val jsonMessage = Message.serialize(message)
    println(jsonMessage)
    jsonMessage.startsWith(s"""{"createdByNode":"Riga","fromPublicKeyEncoded":"$serializedPublicKey","toPublicKeyEncoded":"5678","money":{"currency":"EUR","amountInCents":2025},"timestamp":""") shouldBe true
    jsonMessage.contains("\"encodedSignature\":{}") shouldBe false
  }

  "InitPaymentMessage" should "not create payment message if user signing the message is not found for given (from) public key" in {
    val keyPair = generateKeyPair()
    val serializedPublicKey = serialize(keyPair.getPublic)
    when(keysFileOps.getUserByKey(serializedPublicKey)).thenReturn(None)

    val asset = Money("EUR", 2025)
    InitPaymentMessage("Riga", serializedPublicKey, "5678", asset, keysFileOps ) shouldBe None
  }

}
