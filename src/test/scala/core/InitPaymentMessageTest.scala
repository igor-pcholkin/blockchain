package core

import java.time.LocalDateTime

import keys.{KeysFileOps, KeysGenerator, KeysSerializator}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class InitPaymentMessageTest extends FlatSpec with Matchers with MockitoSugar with KeysGenerator with KeysSerializator {
  val keysFileOps = mock[KeysFileOps]

  "InitPaymentMessage" should "serialize message to json with non empty signature" in {
    val keyPair = generateKeyPair()
    val serializedPublicKey = serialize(keyPair.getPublic)
    when(keysFileOps.readKeyFromFile("Igor/privateKey")).thenReturn(serialize(keyPair.getPrivate))
    when(keysFileOps.readKeyFromFile("Igor/publicKey")).thenReturn(serializedPublicKey)

    val asset = Money("EUR", 2025)
    val message = InitPaymentMessage("Igor", serializedPublicKey, "5678", asset, keysFileOps )

    message.encodedSignature.getOrElse("").length > 0 shouldBe true
    val jsonMessage = message.serialize
    println(jsonMessage)
    jsonMessage.startsWith(s"""{"createdBy":"Igor","fromPublicKeyEncoded":"$serializedPublicKey","toPublicKeyEncoded":"5678","money":{"currency":"EUR","amountInCents":2025},"timestamp":""") shouldBe true
    jsonMessage.contains("\"encodedSignature\":{}") shouldBe false
  }
}
