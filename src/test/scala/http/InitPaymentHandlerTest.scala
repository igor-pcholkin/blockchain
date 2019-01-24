package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime

import business.Money
import com.sun.net.httpserver.HttpExchange
import messages.NewBlockMessage
import core._
import keys.{KeysFileOps, KeysGenerator}
import org.apache.http.HttpStatus.SC_CREATED
import org.mockito.Matchers
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.{PeerAccess, PeerTransport}
import util.{DateTimeUtil, FileOps, StringConverter}

import scala.collection.JavaConverters._
import org.apache.http.HttpStatus
import statements.Payment
import json.FactJson._

class InitPaymentHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with DateTimeUtil with KeysGenerator with StringConverter {
  "InitPaymentHandler" should "initialize new payment to node and send it to another peers" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val localHost = mock[LocalHost]
    val peerAccess = mock[PeerAccess]
    val statementsCache = new StatementsCache
    val blockChain = new TestBlockChain
    val keysFileOps: KeysFileOps = mock[KeysFileOps]

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

    when(peerAccess.localHost).thenReturn(localHost)

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    when(keysFileOps.getUserByKey("Riga", "(publicKeyTo)")).thenReturn(None)
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps, mock[FileOps])
    new InitPaymentHandler(httpContext).handle(mockExchange)

    statementsCache.size shouldBe 1
    blockChain.size shouldBe 1
    val signedStatement = statementsCache.allStatementMessages.head
    val payment = signedStatement.statement.asInstanceOf[Payment]
    payment.createdByNode shouldBe "Riga"
    payment.fromPublicKeyEncoded shouldBe "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    payment.toPublicKeyEncoded shouldBe "(publicKeyTo)"
    payment.money shouldBe Money("EUR", 2025)
    timeStampsAreWithin(payment.timestamp, LocalDateTime.now, 1000) shouldBe true
    val signature = signedStatement.providedSignaturesForKeys.head._2
    val decodedSignature = base64StrToBytes(signature)
    new Signer(keysFileOps).verify("Riga", "Igor", payment.dataToSign, decodedSignature) shouldBe true

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_CREATED),
      Matchers.eq("New Payment has been initiated."))
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(signedStatement))
    verify(blockChain.chainFileOps, never).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])
  }

  it should "create new fact (transaction) in a new block if it could be signed by users on the same node at once" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val keysFileOps: KeysFileOps = mock[KeysFileOps]
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val statementsCache = new StatementsCache
    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val toPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q=="

    val initPaymentRequest =
      s"""{
         | "from": "$fromPublicKey",
         | "to": "$toPublicKey",
         | "currency": "EUR",
         | "amount": 20.25
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(initPaymentRequest.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(peerAccess.localHost).thenReturn(mockLocalHost)

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.getUserByKey("Riga", toPublicKey)).thenReturn(Some("John"))
    // needed to sign payment transaction by public key of local node's owner
    when(keysFileOps.readKeyFromFile("Riga", "John", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAimtA53n1kVMdG1OleLJtfbFnjr1zU5smd04yfbdWpUw==")
    when(keysFileOps.readKeyFromFile("Riga", "John", "publicKey")).thenReturn(toPublicKey)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps, mock[FileOps])
    new InitPaymentHandler(httpContext).handle(mockExchange)

    statementsCache.size shouldBe 0
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New fact has been created and added to blockchain."))
    verify(peerAccess, times(1)).sendMsg(Matchers.any[NewBlockMessage])

    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 2
    val lastBlock = blockChain.getLatestBlock
    val fact = deserialize(new String(lastBlock.data)).right.get
    val secondSignature = base64StrToBytes(fact.providedSignaturesForKeys(1)._2)
    val signer = new Signer(keysFileOps)
    signer.verify("Riga", "John", fact.statement.dataToSign, secondSignature) shouldBe true
    val firstSignature = fact.providedSignaturesForKeys.head._2
    val decodedPaymentMessageSignature = base64StrToBytes(firstSignature)
    signer.verify("Riga", "Igor", fact.statement.dataToSign, decodedPaymentMessageSignature) shouldBe true

  }


  it should "refuse payment request if user signing the message is not found for given (from) public key" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = new PeerAccess(mock[PeerTransport], mockLocalHost)
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val statementsCache = new StatementsCache
    val blockChain = new TestBlockChain
    val keysFileOps: KeysFileOps = mock[KeysFileOps]
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
    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, mock[FileOps])
    new InitPaymentHandler(httpContext).handle(mockExchange)

    statementsCache.size shouldBe 0
    blockChain.size shouldBe 1
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("No user with given (from) public key found."))
    verify(peerAccess.peerTransport, never).sendMsg(Matchers.any[String], Matchers.any[String])
    verify(blockChain.chainFileOps, never).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])
  }
}
