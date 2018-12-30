package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime

import com.sun.net.httpserver.HttpExchange
import core.Block.CURRENT_BLOCK_VERSION
import core._
import messages.{RequestAllStatementsMessage, _}
import io.circe.Encoder
import keys.KeysFileOps
import org.apache.http.HttpStatus
import org.mockito.Matchers
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.PeerAccess
import util.StringConverter
import io.circe.generic.auto._

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

    val initPaymentMessage = InitPaymentMessage("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025), keysFileOps).right.get
    val signedStatement = SignedStatement(initPaymentMessage, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val is = new ByteArrayInputStream(Message.serialize(signedStatement)(SignedStatement.encoder).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("Riga", mockBcHttpServer, statements, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Statement has been verified and added to cache."))
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(signedStatement))(Matchers.any[Encoder[SignedStatement]])
    // it doesn't make disctinction between InitPaymentMessage and NewBlockMessage, so commented out
    //verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])(Matchers.any[Encoder[NewBlockMessage]])

    statements.statements.containsValue(signedStatement) shouldBe true
    blockChain.chain.size() shouldBe 1
  }

  "Message handler" should "reject payment message during failed verification" in {

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

    val initPaymentMessage = InitPaymentMessage("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025), keysFileOps).right.get
    val signedStatement = SignedStatement(initPaymentMessage, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val tamperedMessage = initPaymentMessage.copy(money = Money("EUR", 202500))
    val tamperedStatement = signedStatement.copy(statement = tamperedMessage)
    val is = new ByteArrayInputStream(Message.serialize(tamperedStatement)(SignedStatement.encoder).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("Initial payment message validation failed."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])(Matchers.any[Encoder[NewBlockMessage]])

    statementsCache.statements.containsValue(tamperedStatement) shouldBe false
    blockChain.chain.size() shouldBe 1
  }

  "Message handler" should "create a payment transaction, sign it and add it to newly created block in a blockchain" in {

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

    val paymentMessage = InitPaymentMessage("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025), keysFileOps).right.get
    val signedStatement = SignedStatement(paymentMessage, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    val is = new ByteArrayInputStream(Message.serialize(signedStatement)(SignedStatement.encoder).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Payment transaction created and added to blockchain."))
    verify(peerAccess, times(1)).sendMsg(Matchers.any[NewBlockMessage])(Matchers.any[Encoder[NewBlockMessage]])

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

  "Message handler" should "add a new block to blockchain when it arrives from another node" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val block = Block(CURRENT_BLOCK_VERSION, blockChain.getLatestBlock.hash, LocalDateTime.of(2018, 12, 21, 15, 0, 0), "Hi".getBytes)
    val newBlockMessage = NewBlockMessage(block)
    val is = new ByteArrayInputStream(Message.serialize(newBlockMessage).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(blockChain.chainFileOps.getChainDir("Riga")).thenReturn("Riga/chain")
    when(blockChain.chainFileOps.isChainDirExists("Riga")).thenReturn(true)

    blockChain.chain.size() shouldBe 1

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    blockChain.chain.size() shouldBe 2

    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(0), Matchers.any[Block], Matchers.any[String])
    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(newBlockMessage))(Matchers.any[Encoder[NewBlockMessage]])
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New block received and added to blockchain."))
  }

  "Message handler" should "accept AddPeersMessage when it arrives from another node" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val peers = Seq("localhost:6789", "blabla123.com")
    val addPeersMessage = AddPeersMessage(peers)
    val is = new ByteArrayInputStream(Message.serialize(addPeersMessage).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(peerAccess, times(1)).addAll(Matchers.eq(peers))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New peers received and added to the node."))
  }

  "Message handler" should "accept RequestAllStatementsMessage when it arrives from another node" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val peer = "blabla123.com"
    val requestAllStatementsMessage = RequestAllStatementsMessage(peer)
    val is = new ByteArrayInputStream(Message.serialize(requestAllStatementsMessage).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    val statement1 = new SignedStatement(new TestStatement("a"), Nil)
    val statement2 = new SignedStatement(new TestStatement("b"), Nil)

    statementsCache.add(statement1)
    statementsCache.add(statement2)

    new MsgHandler("Riga", mockBcHttpServer, statementsCache, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(peerAccess, times(1)).sendMsg(Matchers.eq(statement1), Matchers.eq(peer))(Matchers.eq(SignedStatement.encoder))
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(statement2), Matchers.eq(peer))(Matchers.eq(SignedStatement.encoder))
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"All statements have been sent to node: ${peer}."))
  }

}
