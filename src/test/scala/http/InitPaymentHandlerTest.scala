package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime

import com.sun.net.httpserver.HttpExchange
import messages.InitPaymentMessage
import core.{Money, SignedStatement, Signer, StatementsCache}
import keys.{KeysFileOps, KeysGenerator}
import org.apache.http.HttpStatus.SC_CREATED
import org.mockito.Matchers
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.{PeerAccess, PeerTransport}
import util.{DateTimeUtil, StringConverter}

import scala.collection.JavaConverters._
import io.circe._
import org.apache.http.HttpStatus

class InitPaymentHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with DateTimeUtil with KeysGenerator with StringConverter {
  val keysFileOps: KeysFileOps = mock[KeysFileOps]

  "InitPaymentHandler" should "initialize new payment to node and send it to another peers" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val peerAccess = new PeerAccess(mock[PeerTransport])
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val statementsCache = new StatementsCache
    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val initPaymentRequest =
      s"""{
        | "from": "$fromPublicKey",
        | "to": "(publicKeyTo)",
        | "currency": "EUR",
        | "amount": 20.25
        | }
      """.stripMargin
    val is = new ByteArrayInputStream(initPaymentRequest.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    when(keysFileOps.getUserByKey("Riga", "(publicKeyTo)")).thenReturn(None)
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)
    new InitPaymentHandler("Riga", mockBcHttpServer, statementsCache, keysFileOps, peerAccess).handle(mockExchange)

    statementsCache.statements.size shouldBe 1
    val signedStatement = statementsCache.statements.asScala.head._2
    val createdInitPayment = signedStatement.statement.asInstanceOf[InitPaymentMessage]
    createdInitPayment.createdByNode shouldBe "Riga"
    createdInitPayment.fromPublicKeyEncoded shouldBe "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    createdInitPayment.toPublicKeyEncoded shouldBe "(publicKeyTo)"
    createdInitPayment.money shouldBe Money("EUR", 2025)
    timeStampsAreWithin(createdInitPayment.timestamp, LocalDateTime.now, 1000) shouldBe true
    val signature = signedStatement.providedSignaturesForKeys.head._2
    val decodedSignature = base64StrToBytes(signature)
    new Signer(keysFileOps).verify("Riga", "Igor", createdInitPayment.dataToSign, decodedSignature) shouldBe true

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_CREATED),
      Matchers.eq("New Payment has been initiated."))
    verify(peerAccess.peerTransport, times(1)).sendMsg(Matchers.eq(signedStatement), Matchers.eq(Seq("blabla.com", "another.com")))(Matchers.any[Encoder[SignedStatement]])
  }

  "InitPaymentHandler" should "refuse payment request if user signing the message is not found for given (from) public key" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val peerAccess = new PeerAccess(mock[PeerTransport])
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val statementsCache = new StatementsCache
    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val initPaymentRequest =
      s"""{
         | "from": "$fromPublicKey",
         | "to": "(publicKeyTo)",
         | "currency": "EUR",
         | "amount": 20.25
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(initPaymentRequest.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(None)
    when(keysFileOps.getUserByKey("Riga", "(publicKeyTo)")).thenReturn(None)
    new InitPaymentHandler("Riga", mockBcHttpServer, statementsCache, keysFileOps, peerAccess).handle(mockExchange)

    statementsCache.statements.size shouldBe 0
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("No user with given (from) public key found."))
    verify(peerAccess.peerTransport, never).sendMsg(Matchers.any[SignedStatement], Matchers.any[Seq[String]])(Matchers.any[Encoder[SignedStatement]])
  }
}
