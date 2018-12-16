package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.Currency

import com.sun.net.httpserver.HttpExchange
import core.{InitPayments, Money, Signer}
import keys.{KeysFileOps, KeysGenerator, KeysSerializator}
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.{PeerAccess, PeerTransport}
import util.DateTimeUtil

class InitPaymentHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with DateTimeUtil with KeysGenerator with KeysSerializator {
  val keysFileOps = mock[KeysFileOps]

  "InitPaymentHandler" should "initialize new payment to node and send it to another peers" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val peerAccess = new PeerAccess(mock[PeerTransport])
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val initPayments = new InitPayments

    val initPayment =
      """{
        | "from": "A",
        | "to": "B",
        | "currency": "EUR",
        | "amount": 20.25
        | }
      """.stripMargin
    val is = new ByteArrayInputStream(initPayment.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    val nodeName = "Riga"
    val keyPair = generateKeyPair()
    when(keysFileOps.readKeyFromFile("Riga/privateKey")).thenReturn(serialize(keyPair.getPrivate))
    when(keysFileOps.readKeyFromFile("Riga/publicKey")).thenReturn(serialize(keyPair.getPublic))
    new InitPaymentHandler(nodeName, mockBcHttpServer, initPayments, keysFileOps, peerAccess).handle(mockExchange)

    initPayments.initPayments.size shouldBe 1
    val createdInitPayment = initPayments.initPayments.peek()
    createdInitPayment.createdBy shouldBe "Riga"
    createdInitPayment.from shouldBe "A"
    createdInitPayment.to shouldBe "B"
    createdInitPayment.money shouldBe Money("EUR", 2025)
    timeStampsAreWithin(createdInitPayment.timestamp, LocalDateTime.now, 1000) shouldBe true
    new Signer(keysFileOps).verify(nodeName, createdInitPayment, createdInitPayment.fromSignature.getOrElse(Array[Byte]())) shouldBe true

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(201),
      Matchers.eq("New Payment has been initiated."))
    verify(peerAccess.peerTransport, times(1)).sendMsg(Matchers.eq(createdInitPayment), Matchers.eq(Seq("blabla.com", "another.com")))
  }
}
