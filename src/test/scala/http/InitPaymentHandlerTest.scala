package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime

import com.sun.net.httpserver.HttpExchange
import messages.{NewBlockMessage, SignedStatementMessage}
import core._
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
import statements.InitPayment

class InitPaymentHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with DateTimeUtil with KeysGenerator with StringConverter {
  val keysFileOps: KeysFileOps = mock[KeysFileOps]

  "InitPaymentHandler" should "initialize new payment to node and send it to another peers" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val peerAccess = new PeerAccess(mock[PeerTransport])
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val statementsCache = new StatementsCache
    val blockChain = new TestBlockChain
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
    new InitPaymentHandler("Riga", mockBcHttpServer, statementsCache, keysFileOps, peerAccess, blockChain).handle(mockExchange)

    statementsCache.statements.size shouldBe 1
    blockChain.chain.size shouldBe 1
    val signedStatement = statementsCache.statements.asScala.head._2
    val createdInitPayment = signedStatement.statement.asInstanceOf[InitPayment]
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
    verify(peerAccess.peerTransport, times(1)).sendMsg(Matchers.eq(signedStatement), Matchers.eq(Seq("blabla.com", "another.com")))(Matchers.any[Encoder[SignedStatementMessage]])
  }

  "InitPaymentHandler" should "create new fact (transaction) in a new block if it could be signed by users on the same node at once" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
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


    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.getUserByKey("Riga", toPublicKey)).thenReturn(Some("John"))
    // needed to sign payment transaction by public key of local node's owner
    when(keysFileOps.readKeyFromFile("Riga", "John", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAimtA53n1kVMdG1OleLJtfbFnjr1zU5smd04yfbdWpUw==")
    when(keysFileOps.readKeyFromFile("Riga", "John", "publicKey")).thenReturn(toPublicKey)

    new InitPaymentHandler("Riga", mockBcHttpServer, statementsCache, keysFileOps, peerAccess, blockChain).handle(mockExchange)

    statementsCache.statements.size shouldBe 0
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Payment transaction created and added to blockchain."))
    verify(peerAccess, times(1)).sendMsg(Matchers.any[NewBlockMessage])(Matchers.any[Encoder[NewBlockMessage]])

    blockChain.chain.size() shouldBe 2
    val lastBlock = blockChain.getLatestBlock
    val fact = Fact.deserialize(new String(lastBlock.data)).right.get
    val secondSignature = base64StrToBytes(fact.providedSignaturesForKeys(1)._2)
    val signer = new Signer(keysFileOps)
    signer.verify("Riga", "John", fact.statement.dataToSign, secondSignature) shouldBe true
    val firstSignature = fact.providedSignaturesForKeys.head._2
    val decodedPaymentMessageSignature = base64StrToBytes(firstSignature)
    signer.verify("Riga", "Igor", fact.statement.dataToSign, decodedPaymentMessageSignature) shouldBe true

  }


  "InitPaymentHandler" should "refuse payment request if user signing the message is not found for given (from) public key" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val peerAccess = new PeerAccess(mock[PeerTransport])
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val statementsCache = new StatementsCache
    val blockChain = new TestBlockChain
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
    new InitPaymentHandler("Riga", mockBcHttpServer, statementsCache, keysFileOps, peerAccess, blockChain).handle(mockExchange)

    statementsCache.statements.size shouldBe 0
    blockChain.chain.size shouldBe 1
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("No user with given (from) public key found."))
    verify(peerAccess.peerTransport, never).sendMsg(Matchers.any[SignedStatementMessage], Matchers.any[Seq[String]])(Matchers.any[Encoder[SignedStatementMessage]])
  }
}
