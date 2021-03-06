package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime

import business.Money
import com.sun.net.httpserver.HttpExchange
import core.Block.CURRENT_BLOCK_VERSION
import core._
import messages._
import keys.KeysFileOps
import org.apache.http.HttpStatus
import org.mockito.Matchers
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.PeerAccess
import json.{FactJson, JsonSerializer}
import util.{DateTimeUtil, FileOps, StringConverter}
import statements.{Payment, RegisteredUser}
import json.FactJson._
import json.MessageEnvelopeJson.envelopeEncoder

class MsgHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with StringConverter
  with DateTimeUtil {

  "Message handler" should "verify, add initial payment message to message cache and relay to peers, without creation payment transaction" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val toPublicKey = "(publicKeyTo)"

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.getUserByKey("Riga", toPublicKey)).thenReturn(None)

    val payment = Payment.verifyAndCreate("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025)).right.get
    val signedStatement = SignedStatementMessage(payment, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val messageEnvelope = MessageEnvelope(signedStatement, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Statement has been verified and added to cache."))
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(signedStatement))
    // it doesn't make disctinction between SignedStatementMessage and NewBlockMessage, so commented out
    //verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])(Matchers.any[Encoder[NewBlockMessage]])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))

    statementsCache.contains(signedStatement) shouldBe true
    blockChain.size shouldBe 1
  }

  it should "reject payment message during failed verification" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val toPublicKey = "(publicKeyTo)"

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.getUserByKey("Riga", toPublicKey)).thenReturn(None)

    val payment = Payment.verifyAndCreate("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025)).right.get
    val signedStatement = SignedStatementMessage(payment, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val tamperedPayment = payment.copy(money = Money("EUR", 202500))
    val tamperedSignedStatement = signedStatement.copy(statement = tamperedPayment)
    val messageEnvelope = MessageEnvelope(tamperedSignedStatement, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(HttpStatus.SC_BAD_REQUEST), Matchers.eq("Statement validation failed."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))

    statementsCache.contains(tamperedSignedStatement) shouldBe false
    blockChain.size shouldBe 1

  }

  it should "reject processing the same repeated statement if it is wrapped in the same signed message" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val toPublicKey = "(publicKeyTo)"

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.getUserByKey("Riga", toPublicKey)).thenReturn(None)

    val payment = Payment.verifyAndCreate("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025)).right.get
    val signedStatement = SignedStatementMessage(payment, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val messageEnvelope = MessageEnvelope(signedStatement, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    statementsCache.add(signedStatement)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(HttpStatus.SC_BAD_REQUEST), Matchers.eq("The statement has been received before."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))

    blockChain.size shouldBe 1
  }

  it should "reject the same repeated statement if it is wrapped in another signed message" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val toPublicKey = "(publicKeyTo)"

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.getUserByKey("Riga", toPublicKey)).thenReturn(None)

    val payment = Payment.verifyAndCreate("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025)).right.get
    val signedStatement = SignedStatementMessage(payment, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val messageEnvelope = MessageEnvelope(signedStatement, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    statementsCache.add(signedStatement.copy(providedSignaturesForKeys = Nil))

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(HttpStatus.SC_BAD_REQUEST), Matchers.eq("The statement has been received before."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))

    blockChain.size shouldBe 1
  }


  it should "create a payment transaction, sign it and add it to newly created block in a blockchain" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = mock[PeerAccess]

    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val toPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q=="

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.getUserByKey("Riga", toPublicKey)).thenReturn(Some("John"))
    // needed to sign payment transaction by public key of local node's owner
    when(keysFileOps.readKeyFromFile("Riga", "John", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAimtA53n1kVMdG1OleLJtfbFnjr1zU5smd04yfbdWpUw==")
    when(keysFileOps.readKeyFromFile("Riga", "John", "publicKey")).thenReturn(toPublicKey)

    val payment = Payment.verifyAndCreate("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025)).right.get
    val signedStatement = SignedStatementMessage(payment, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val messageEnvelope = MessageEnvelope(signedStatement, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(peerAccess.localHost).thenReturn(mockLocalHost)
    when(mockLocalHost.localServerAddress).thenReturn("localhost")

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New fact has been created and added to blockchain."))
    verify(peerAccess, times(1)).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))

    statementsCache.contains(signedStatement) shouldBe false
    blockChain.size shouldBe 2
    val lastBlock = blockChain.getLatestBlock
    val fact = FactJson.deserialize(new String(lastBlock.data)).right.get
    val secondSignature = base64StrToBytes(fact.providedSignaturesForKeys(1)._2)
    val signer = new Signer(keysFileOps)
    signer.verify("Riga", "John", fact.statement.dataToSign, secondSignature) shouldBe true
    val firstSignature = fact.providedSignaturesForKeys.head._2
    val decodedPaymentMessageSignature = base64StrToBytes(firstSignature)
    signer.verify("Riga", "Igor", fact.statement.dataToSign, decodedPaymentMessageSignature) shouldBe true
  }

  it should "refuse to create a payment transaction if related fact is already in blockchain" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val payment = Payment.verifyAndCreate("Riga", "fromPublicKey", "toPublicKey", Money("EUR", 2025)).right.get
    val signedStatement = SignedStatementMessage(payment, Nil)
    val messageEnvelope = MessageEnvelope(signedStatement, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    val fact = Fact(signedStatement.statement, signedStatement.providedSignaturesForKeys)
    val serializedFact = JsonSerializer.serialize(fact).getBytes
    val block = Block(CURRENT_BLOCK_VERSION, blockChain.getLatestBlock.hash, LocalDateTime.of(2018, 12, 21, 15, 0, 0),
      serializedFact)
    blockChain.add(block)
    blockChain.size shouldBe 2

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("The statement refused: blockchain alrady contains it as a fact."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))

    statementsCache.contains(signedStatement) shouldBe false
    blockChain.size shouldBe 2
  }

  it should "add a new block to blockchain when it arrives from another node" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val toPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q=="

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.getUserByKey("Riga", toPublicKey)).thenReturn(Some("John"))
    // needed to sign payment transaction by public key of local node's owner
    when(keysFileOps.readKeyFromFile("Riga", "John", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAimtA53n1kVMdG1OleLJtfbFnjr1zU5smd04yfbdWpUw==")
    when(keysFileOps.readKeyFromFile("Riga", "John", "publicKey")).thenReturn(toPublicKey)

    val payment = Payment.verifyAndCreate("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025)).right.get
    val signedStatement = SignedStatementMessage(payment, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)

    val fact = Fact(signedStatement.statement, signedStatement.providedSignaturesForKeys)
    val serializedFact = JsonSerializer.serialize(fact).getBytes
    val block = Block(CURRENT_BLOCK_VERSION, blockChain.getLatestBlock.hash, LocalDateTime.of(2018, 12, 21, 15, 0, 0),
      serializedFact)
    val newBlockMessage = NewBlockMessage(block, 1)
    val messageEnvelope = MessageEnvelope(newBlockMessage, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    statementsCache.add(signedStatement)
    statementsCache.contains(signedStatement) shouldBe true

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(blockChain.chainFileOps.getChainDir("Riga")).thenReturn("Riga/chain")
    when(blockChain.chainFileOps.isChainDirExists("Riga")).thenReturn(true)

    blockChain.size shouldBe 1

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    blockChain.size shouldBe 2
    statementsCache.contains(signedStatement) shouldBe false

    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(0), Matchers.any[Block], Matchers.any[String])
    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(newBlockMessage))
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New block received and added to blockchain."))
  }

  it should "refuse to add a new block to blockchain when it arrives from another node and fails verification" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val toPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q=="

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.getUserByKey("Riga", toPublicKey)).thenReturn(Some("John"))
    // needed to sign payment transaction by public key of local node's owner
    when(keysFileOps.readKeyFromFile("Riga", "John", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAimtA53n1kVMdG1OleLJtfbFnjr1zU5smd04yfbdWpUw==")
    when(keysFileOps.readKeyFromFile("Riga", "John", "publicKey")).thenReturn(toPublicKey)

    val payment = Payment.verifyAndCreate("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025)).right.get
    val signedStatement = SignedStatementMessage(payment, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val tamperedPayment = payment.copy(money = Money("EUR", 202500))

    val invalidFact = Fact(tamperedPayment, signedStatement.providedSignaturesForKeys)
    val serializedFact = JsonSerializer.serialize(invalidFact).getBytes
    val block = Block(CURRENT_BLOCK_VERSION, blockChain.getLatestBlock.hash, LocalDateTime.of(2018, 12, 21, 15, 0, 0),
      serializedFact)
    val newBlockMessage = NewBlockMessage(block, 1)
    val messageEnvelope = MessageEnvelope(newBlockMessage, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(blockChain.chainFileOps.getChainDir("Riga")).thenReturn("Riga/chain")
    when(blockChain.chainFileOps.isChainDirExists("Riga")).thenReturn(true)

    blockChain.size shouldBe 1

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    blockChain.size shouldBe 1

    verify(blockChain.chainFileOps, never).writeBlock(Matchers.eq(0), Matchers.any[Block], Matchers.any[String])
    verify(blockChain.chainFileOps, never).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])
    verify(peerAccess, never).sendMsg(Matchers.eq(newBlockMessage))
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("Verification of signatures failed for fact in received block."))
  }

  it should "add new block with new index to blockchain when it arrives from another node and it's date is newer than of existing block with the same index" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val payment = Payment.verifyAndCreate("Riga", "fromPublicKey", "toPublicKey", Money("EUR", 2025)).right.get
    val signedStatement = SignedStatementMessage(payment, Nil)
    val fact = Fact(signedStatement.statement, signedStatement.providedSignaturesForKeys)
    val serializedFact = JsonSerializer.serialize(fact).getBytes
    val block = Block(CURRENT_BLOCK_VERSION, blockChain.getLatestBlock.hash, LocalDateTime.of(2018, 12, 21, 15, 0, 0),
      serializedFact)
    val newBlockMessage = NewBlockMessage(block, 1)
    val messageEnvelope = MessageEnvelope(newBlockMessage, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    statementsCache.add(signedStatement)
    statementsCache.contains(signedStatement) shouldBe true

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(blockChain.chainFileOps.getChainDir("Riga")).thenReturn("Riga/chain")
    when(blockChain.chainFileOps.isChainDirExists("Riga")).thenReturn(true)

    val block1 = blockChain.genNextBlock("Fund transfer from A to B".getBytes, LocalDateTime.of(2018, 12, 13, 23, 0, 0))
    blockChain.add(block1)

    blockChain.size shouldBe 2

    val prevBlock1Hash = blockChain.blockAt(1).hash

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps,
      mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    blockChain.size shouldBe 3

    statementsCache.contains(signedStatement) shouldBe false

    val blocks = blockChain.blocksFrom(0)
    new String(blocks(1).data) shouldBe "Fund transfer from A to B"
    blockChain.extractFact(blocks(2)).right.get.statement shouldBe payment
    blocks(1).hash shouldBe prevBlock1Hash
    blocks(2).prevHash shouldBe blocks(1).hash
    timeStampsAreWithin(blocks(2).timestamp, LocalDateTime.now(), 1000) shouldBe true

    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(0), Matchers.any[Block], Matchers.any[String])
    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])
    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(2), Matchers.any[Block], Matchers.any[String])
    verify(peerAccess, times(2)).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New block received and added to blockchain with adjusted index."))
  }

  it should "replace a block in blockchain when another another block with the same index arrives from another node and it's date is older than of existing block with the same index" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val payment = Payment.verifyAndCreate("Riga", "fromPublicKey", "toPublicKey", Money("EUR", 2025)).right.get
    val signedStatement = SignedStatementMessage(payment, Nil)
    val fact = Fact(signedStatement.statement, signedStatement.providedSignaturesForKeys)
    val serializedFact = JsonSerializer.serialize(fact).getBytes
    val block = Block(CURRENT_BLOCK_VERSION, blockChain.getLatestBlock.hash, LocalDateTime.of(2018, 12, 13, 23, 0, 0),
      serializedFact)
    val newBlockMessage = NewBlockMessage(block, 1)
    val messageEnvelope = MessageEnvelope(newBlockMessage, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    statementsCache.add(signedStatement)
    statementsCache.contains(signedStatement) shouldBe true

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(blockChain.chainFileOps.getChainDir("Riga")).thenReturn("Riga/chain")
    when(blockChain.chainFileOps.isChainDirExists("Riga")).thenReturn(true)

    val block1 = blockChain.genNextBlock("Fund transfer from A to B".getBytes, LocalDateTime.of(2018, 12, 21, 15, 0, 0))
    blockChain.add(block1)

    blockChain.size shouldBe 2

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps,
      mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    blockChain.size shouldBe 2

    statementsCache.contains(signedStatement) shouldBe false

    val blocks = blockChain.blocksFrom(0)
    blockChain.extractFact(blocks(1)).right.get.statement shouldBe payment

    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(0), Matchers.any[Block], Matchers.any[String])
    verify(blockChain.chainFileOps, times(1)).deleteBlock(Matchers.eq(1), Matchers.any[String])
    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])
    verify(peerAccess, times(2)).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New block received and inserted to blockchain adjusting existing blocks."))
  }

  it should "not add new block to blockchain when it arrives from another node and contains an existing fact" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val statement = RegisteredUser("Igor", "ipcholkin@gmail.com", "(UserPublicKey)")
    val signedStatement = SignedStatementMessage(statement, Nil)
    val fact = Fact(signedStatement.statement, signedStatement.providedSignaturesForKeys)
    val serializedFact = JsonSerializer.serialize(fact).getBytes
    val block = blockChain.genNextBlock(serializedFact)
    blockChain.add(block)

    val newBlockMessage = NewBlockMessage(block, 1)
    val messageEnvelope = MessageEnvelope(newBlockMessage, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    blockChain.size shouldBe 2

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps,
      mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    blockChain.size shouldBe 2

    verify(blockChain.chainFileOps, never).writeBlock(Matchers.anyInt, Matchers.any[Block], Matchers.any[String])
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("New block refused - contains existing fact."))
  }

  it should "not reinsert new block with index 0 to blockchain when it arrives from another node and it's date is older" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val block = Block(CURRENT_BLOCK_VERSION, blockChain.getLatestBlock.hash, LocalDateTime.of(2018, 12, 1, 23, 0, 0),
      "Hi".getBytes)
    val newBlockMessage = NewBlockMessage(block, 1)
    val messageEnvelope = MessageEnvelope(newBlockMessage, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(blockChain.chainFileOps.getChainDir("Riga")).thenReturn("Riga/chain")
    when(blockChain.chainFileOps.isChainDirExists("Riga")).thenReturn(true)

    blockChain.size shouldBe 1

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps,
      mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    blockChain.size shouldBe 1

    val blocks = blockChain.blocksFrom(0)
    blocks.head shouldBe blockChain.origin

    verify(blockChain.chainFileOps, never).writeBlock(Matchers.anyInt, Matchers.any[Block], Matchers.any[String])
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("Invalid block received - rejected."))
  }

  it should "accept AddPeersMessage when it arrives from another node" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val peers = Seq("localhost:6789", "blabla123.com")
    val addPeersMessage = AddPeersMessage(peers)
    val messageEnvelope = MessageEnvelope(addPeersMessage, "localhost")
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps,
      mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    verify(peerAccess, times(1)).addAll(Matchers.eq(peers))
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New peers received and added to the node."))
  }

  it should "accept PullNewsMessage when it arrives from another node" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]
    val mockLocalHost = mock[LocalHost]

    val peer = "blabla123.com"
    val pullNewsMessage = PullNewsMessage(2)
    val messageEnvelope = MessageEnvelope(pullNewsMessage, peer)
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    val statement1 = new SignedStatementMessage(TestStatement("a"), Nil)
    val statement2 = new SignedStatementMessage(TestStatement("b"), Nil)
    statementsCache.add(statement1)
    statementsCache.add(statement2)

    val newBlock1 = blockChain.genNextBlock("Fund transfer from A to B".getBytes)
    blockChain.add(newBlock1)
    val newBlock2 = blockChain.genNextBlock("Fund transfer from B to C".getBytes)
    blockChain.add(newBlock2)
    val newBlock3 = blockChain.genNextBlock("Fund transfer from C to D".getBytes)
    blockChain.add(newBlock3)

    when(peerAccess.localHost).thenReturn(mockLocalHost)
    when(mockLocalHost.localServerAddress).thenReturn("localhost")

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps,
      mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    verify(peerAccess, times(1)).sendMsg(Matchers.eq(statement1), Matchers.eq(peer))
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(statement2), Matchers.eq(peer))

    verify(peerAccess, times(1)).sendMsg(Matchers.eq(NewBlockMessage(newBlock2, 2)), Matchers.eq(peer))
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(NewBlockMessage(newBlock3, 3)), Matchers.eq(peer))
    verify(peerAccess, times(1)).add(Matchers.eq(peer))
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(PullNewsMessage(4, inReply = true)), Matchers.eq(peer))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"All statements and blocks have been sent to node: $peer."))
  }

  it should "not reply with PullNewsMessage when PullNewsMessage arrives from another node and that message is marked with inReply flag" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]
    val mockLocalHost = mock[LocalHost]

    val peer = "blabla123.com"
    val pullNewsMessage = PullNewsMessage(2, inReply = true)
    val messageEnvelope = MessageEnvelope(pullNewsMessage, peer)
    val is = new ByteArrayInputStream(JsonSerializer.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(peerAccess.localHost).thenReturn(mockLocalHost)
    when(mockLocalHost.localServerAddress).thenReturn("localhost")

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps,
      mock[FileOps])
    new MsgHandler(httpContext).handle(mockExchange)

    verify(peerAccess, times(1)).add(Matchers.eq(peer))
    verify(peerAccess, never).sendMsg(Matchers.eq(PullNewsMessage(1)), Matchers.eq(peer))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"All statements and blocks have been sent to node: $peer."))
  }

}
