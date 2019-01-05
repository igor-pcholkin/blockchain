package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime

import com.sun.net.httpserver.HttpExchange
import core.Block.CURRENT_BLOCK_VERSION
import core._
import messages.{RequestAllStatementsMessage, _}
import serialization.MessageEnvelopeOps._
import keys.KeysFileOps
import org.apache.http.HttpStatus
import org.mockito.Matchers
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.PeerAccess
import util.StringConverter
import statements.InitPayment

class MsgHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with StringConverter {

  "Message handler" should "verify, add initial payment message to message cache and relay to peers, without creation payment transaction" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statements = new StatementsCache()
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

    val initPayment = InitPayment("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025), keysFileOps).right.get
    val signedStatement = SignedStatementMessage(initPayment, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val messageEnvelope = MessageEnvelope(signedStatement, "localhost")
    val is = new ByteArrayInputStream(Serializator.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("Riga", mockBcHttpServer, statements, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Statement has been verified and added to cache."))
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(signedStatement))
    // it doesn't make disctinction between SignedStatementMessage and NewBlockMessage, so commented out
    //verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])(Matchers.any[Encoder[NewBlockMessage]])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))

    statements.statements.containsValue(signedStatement) shouldBe true
    blockChain.chain.size() shouldBe 1
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

    val initPayment = InitPayment("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025), keysFileOps).right.get
    val signedStatement = SignedStatementMessage(initPayment, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val tamperedMessage = initPayment.copy(money = Money("EUR", 202500))
    val tamperedStatement = signedStatement.copy(statement = tamperedMessage)
    val messageEnvelope = MessageEnvelope(tamperedStatement, "localhost")
    val is = new ByteArrayInputStream(Serializator.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("Initial payment message validation failed."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))

    statementsCache.statements.containsValue(tamperedStatement) shouldBe false
    blockChain.chain.size() shouldBe 1
  }

  it should "reject repeated payment message" in {

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

    val initPayment = InitPayment("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025), keysFileOps).right.get
    val signedStatement = SignedStatementMessage(initPayment, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val messageEnvelope = MessageEnvelope(signedStatement, "localhost")
    val is = new ByteArrayInputStream(Serializator.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    statementsCache.add(signedStatement)

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("The statement has been received before."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))

    blockChain.chain.size() shouldBe 1
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

    val initPayment = InitPayment("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025), keysFileOps).right.get
    val signedStatement = SignedStatementMessage(initPayment, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val messageEnvelope = MessageEnvelope(signedStatement, "localhost")
    val is = new ByteArrayInputStream(Serializator.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(peerAccess.localHost).thenReturn(mockLocalHost)
    when(mockLocalHost.localServerAddress).thenReturn("localhost")

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Payment transaction created and added to blockchain."))
    verify(peerAccess, times(1)).sendMsg(Matchers.any[NewBlockMessage])
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))

    statementsCache.statements.containsValue(signedStatement) shouldBe true
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

  it should "add a new block to blockchain when it arrives from another node" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val block = Block(CURRENT_BLOCK_VERSION, blockChain.getLatestBlock.hash, LocalDateTime.of(2018, 12, 21, 15, 0, 0), "Hi".getBytes)
    val newBlockMessage = NewBlockMessage(block, 1)
    val messageEnvelope = MessageEnvelope(newBlockMessage, "localhost")
    val is = new ByteArrayInputStream(Serializator.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(blockChain.chainFileOps.getChainDir("Riga")).thenReturn("Riga/chain")
    when(blockChain.chainFileOps.isChainDirExists("Riga")).thenReturn(true)

    blockChain.chain.size() shouldBe 1

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    blockChain.chain.size() shouldBe 2

    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(0), Matchers.any[Block], Matchers.any[String])
    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(newBlockMessage))
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New block received and added to blockchain."))
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
    val is = new ByteArrayInputStream(Serializator.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(peerAccess, times(1)).addAll(Matchers.eq(peers))
    verify(peerAccess, times(1)).add(Matchers.eq("localhost"))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New peers received and added to the node."))
  }

  it should "accept RequestAllStatementsMessage when it arrives from another node" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val peer = "blabla123.com"
    val requestAllStatementsMessage = RequestAllStatementsMessage()
    val messageEnvelope = MessageEnvelope(requestAllStatementsMessage, peer)
    val is = new ByteArrayInputStream(Serializator.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    val statement1 = new SignedStatementMessage(TestStatement("a"), Nil)
    val statement2 = new SignedStatementMessage(TestStatement("b"), Nil)

    statementsCache.add(statement1)
    statementsCache.add(statement2)

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(peerAccess, times(1)).sendMsg(Matchers.eq(statement1), Matchers.eq(peer))
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(statement2), Matchers.eq(peer))
    verify(peerAccess, times(1)).add(Matchers.eq(peer))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"All statements have been sent to node: $peer."))
  }

  it should "accept RequestBlocksMessage when it arrives from another node" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]
    val mockLocalHost = mock[LocalHost]

    val peer = "blabla123.com"
    val requestBlocksMessage = RequestBlocksMessage(2)
    val messageEnvelope = MessageEnvelope(requestBlocksMessage, peer)
    val is = new ByteArrayInputStream(Serializator.serialize(messageEnvelope).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    val newBlock1 = blockChain.genNextBlock("Fund transfer from A to B".getBytes)
    blockChain.add(newBlock1)
    val newBlock2 = blockChain.genNextBlock("Fund transfer from B to C".getBytes)
    blockChain.add(newBlock2)
    val newBlock3 = blockChain.genNextBlock("Fund transfer from C to D".getBytes)
    blockChain.add(newBlock3)

    when(peerAccess.localHost).thenReturn(mockLocalHost)
    when(mockLocalHost.localServerAddress).thenReturn("localhost")

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(peerAccess, times(1)).sendMsg(Matchers.eq(NewBlockMessage(newBlock2, 2)), Matchers.eq(peer))
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(NewBlockMessage(newBlock3, 3)), Matchers.eq(peer))
    verify(peerAccess, times(1)).add(Matchers.eq(peer))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"All requested blocks have been sent to node: $peer."))
  }


}
