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
    when(keysFileOps.readKeyFromFile("Igor/privateKey")).thenReturn(serialize(keyPair.getPrivate))
    when(keysFileOps.readKeyFromFile("Igor/publicKey")).thenReturn(serialize(keyPair.getPublic))

    val asset = Money("EUR", 2025)
    val notSignedMessage = InitPaymentMessage("Igor", "Igor", "John", asset, LocalDateTime.now )

    val signer = new Signer(keysFileOps)
    val initPaymentMessageSigned = notSignedMessage.copy( fromSignature = Some(signer.sign("Igor", notSignedMessage)))
    initPaymentMessageSigned.fromSignature.nonEmpty shouldBe true
    initPaymentMessageSigned.fromSignature.getOrElse(Array[Byte]()).length > 0 shouldBe true
    println(initPaymentMessageSigned)
    initPaymentMessageSigned.serialize.contains("\"fromSignature\":{}") shouldBe false
  }
}
