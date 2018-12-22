package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime

import com.sun.net.httpserver.HttpExchange
import core.messages.InitPaymentMessage
import core.{InitPayments, Money, Signer}
import keys.{KeysFileOps, KeysGenerator, KeysSerializator}
import org.apache.http.HttpStatus.SC_CREATED
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.{PeerAccess, PeerTransport}
import util.{DateTimeUtil, StringConverter}

import scala.collection.JavaConverters._
import io.circe._

class InitPaymentHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with DateTimeUtil with KeysGenerator with StringConverter {
  val keysFileOps = mock[KeysFileOps]

  "InitPaymentHandler" should "initialize new payment to node and send it to another peers" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val peerAccess = new PeerAccess(mock[PeerTransport])
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val initPayments = new InitPayments
    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val initPayment =
      s"""{
        | "from": "$fromPublicKey",
        | "to": "(publicKeyTo)",
        | "currency": "EUR",
        | "amount": 20.25
        | }
      """.stripMargin
    val is = new ByteArrayInputStream(initPayment.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    val nodeName = "Riga"
    when(keysFileOps.getUserByKey(fromPublicKey)).thenReturn(Some("Igor"))
    when(keysFileOps.readKeyFromFile("keys/Igor/privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("keys/Igor/publicKey")).thenReturn(fromPublicKey)
    new InitPaymentHandler(nodeName, mockBcHttpServer, initPayments, keysFileOps, peerAccess).handle(mockExchange)

    initPayments.initPayments.size shouldBe 1
    val createdInitPayment = initPayments.initPayments.asScala.head._2
    createdInitPayment.createdByNode shouldBe "Riga"
    createdInitPayment.fromPublicKeyEncoded shouldBe "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    createdInitPayment.toPublicKeyEncoded shouldBe "(publicKeyTo)"
    createdInitPayment.money shouldBe Money("EUR", 2025)
    timeStampsAreWithin(createdInitPayment.timestamp, LocalDateTime.now, 1000) shouldBe true
    val decodedSignature = base64StrToBytes(createdInitPayment.encodedSignature.getOrElse(""))
    new Signer(keysFileOps).verify("Igor", createdInitPayment.dataToSign, decodedSignature) shouldBe true

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_CREATED),
      Matchers.eq("New Payment has been initiated."))
    verify(peerAccess.peerTransport, times(1)).sendMsg(Matchers.eq(createdInitPayment), Matchers.eq(Seq("blabla.com", "another.com")))(Matchers.any[Encoder[InitPaymentMessage]])
  }
}
