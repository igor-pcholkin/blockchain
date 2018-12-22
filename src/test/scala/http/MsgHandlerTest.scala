package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime

import com.sun.net.httpserver.HttpExchange
import core._
import core.messages.{InitPaymentMessage, Message, NewBlockMessage, PaymentTransaction}
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

  "Message handler" should "verify and add initial payment message to message cache without creation payment transaction" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new BlockChain
    val initPayments = new InitPayments()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="

    when(keysFileOps.getUserByKey(fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("keys/Igor/privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("keys/Igor/publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.isKeysDirExists("John")).thenReturn(false)

    val signedMessage = InitPaymentMessage.apply("Riga", fromPublicKey, "(publicKeyTo)", Money("EUR", 2025), keysFileOps).right.get
    val is = new ByteArrayInputStream(Message.serialize(signedMessage).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("John", mockBcHttpServer, initPayments, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Initial payment message verified and added to message cache."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])(Matchers.any[Encoder[NewBlockMessage]])

    initPayments.initPayments.containsValue(signedMessage) shouldBe true
    blockChain.chain.size() shouldBe 1
  }

  "Message handler" should "reject payment message during failed verification" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new BlockChain
    val initPayments = new InitPayments()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="

    when(keysFileOps.getUserByKey(fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("keys/Igor/privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("keys/Igor/publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.isKeysDirExists("John")).thenReturn(false)

    val signedMessage = InitPaymentMessage.apply("Riga", fromPublicKey, "(publicKeyTo)", Money("EUR", 2025), keysFileOps).right.get
    val tamperedMessage = signedMessage.copy(money = Money("EUR", 202500))
    val is = new ByteArrayInputStream(Message.serialize(tamperedMessage).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("John", mockBcHttpServer, initPayments, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("Initial payment message validation failed."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])(Matchers.any[Encoder[NewBlockMessage]])

    initPayments.initPayments.containsValue(signedMessage) shouldBe false
    blockChain.chain.size() shouldBe 1
  }

  "Message handler" should "create a payment transaction, sign it and add it to newly created block in a blockchain" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new BlockChain
    val initPayments = new InitPayments()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="

    when(keysFileOps.getUserByKey(fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("keys/Igor/privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("keys/Igor/publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.isKeysDirExists("John")).thenReturn(true)
    // needed to sign payment transaction by public key of local node's owner
    when(keysFileOps.readKeyFromFile("keys/John/privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAimtA53n1kVMdG1OleLJtfbFnjr1zU5smd04yfbdWpUw==")
    when(keysFileOps.readKeyFromFile("keys/John/publicKey")).thenReturn("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q==")

    val signedMessage = InitPaymentMessage.apply("Riga", fromPublicKey,
      "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q==", Money("EUR", 2025), keysFileOps).right.get
    val is = new ByteArrayInputStream(Message.serialize(signedMessage).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("John", mockBcHttpServer, initPayments, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Payment transaction created and added to blockchain."))
    verify(peerAccess, times(1)).sendMsg(Matchers.any[NewBlockMessage])(Matchers.any[Encoder[NewBlockMessage]])

    initPayments.initPayments.containsValue(signedMessage) shouldBe true
    blockChain.chain.size() shouldBe 2
    val lastBlock = blockChain.getLatestBlock
    val transaction = PaymentTransaction.deserialize(new String(lastBlock.data)).right.get
    transaction.paymentMessage shouldBe signedMessage
    val decodedTransactionSignature = base64StrToBytes(transaction.encodedSignature.get)
    val signer = new Signer(keysFileOps)
    signer.verify("John", transaction.dataToSign, decodedTransactionSignature) shouldBe true
    val decodedPaymentMessageSignature = base64StrToBytes(transaction.paymentMessage.encodedSignature.get)
    signer.verify("Igor", transaction.paymentMessage.dataToSign, decodedPaymentMessageSignature) shouldBe true
  }

  "Message handler" should "add a new block to blockchain when it arrives from another node" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new BlockChain
    val initPayments = new InitPayments()
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]

    val block = Block(1, blockChain.getLatestBlock.hash, LocalDateTime.of(2018, 12, 21, 15, 0, 0), "Hi".getBytes)
    val newBlockMessage = NewBlockMessage(block)
    val is = new ByteArrayInputStream(Message.serialize(newBlockMessage).getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    blockChain.chain.size() shouldBe 1

    new MsgHandler("Riga", mockBcHttpServer, initPayments, blockChain, keysFileOps, peerAccess).handle(mockExchange)

    blockChain.chain.size() shouldBe 2

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New block received and added to blockchain."))
  }

}
