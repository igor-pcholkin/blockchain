package http

import java.io.ByteArrayInputStream

import com.sun.net.httpserver.HttpExchange
import core._
import keys.KeysFileOps
import org.apache.http.HttpStatus
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import util.StringConverter

class MsgHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with StringConverter {

  "Message handler" should "verify and add initial payment message to message cache without creation payment transaction" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new BlockChain
    val initPayments = new InitPayments()
    val keysFileOps = mock[KeysFileOps]

    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Igor/privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Igor/publicKey")).thenReturn("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==")
    // whether payment transaction could be created and signed
    when(keysFileOps.isKeysDirExists("John")).thenReturn(false)

    val signedMessage = InitPaymentMessage.apply("Igor", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==",
      "(publicKeyTo)", Money("EUR", 2025), keysFileOps)
    val is = new ByteArrayInputStream(signedMessage.serialize.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("John", mockBcHttpServer, initPayments, blockChain, keysFileOps).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Initial payment message verified and added to message cache."))

    initPayments.initPayments.containsValue(signedMessage) shouldBe true
    blockChain.chain.size() shouldBe 1
  }

  "Message handler" should "reject payment message during failed verification" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new BlockChain
    val initPayments = new InitPayments()
    val keysFileOps = mock[KeysFileOps]

    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Igor/privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Igor/publicKey")).thenReturn("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==")
    // whether payment transaction could be created and signed
    when(keysFileOps.isKeysDirExists("John")).thenReturn(false)

    val signedMessage = InitPaymentMessage.apply("Igor", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==",
      "(publicKeyTo)", Money("EUR", 2025), keysFileOps)
    val tamperedMessage = signedMessage.copy(money = Money("EUR", 202500))
    val is = new ByteArrayInputStream(tamperedMessage.serialize.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("John", mockBcHttpServer, initPayments, blockChain, keysFileOps).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("Initial payment message validation failed."))

    initPayments.initPayments.containsValue(signedMessage) shouldBe false
    blockChain.chain.size() shouldBe 1
  }

  "Message handler" should "create a payment transaction, sign it and add it to newly created block in a blockchain" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new BlockChain
    val initPayments = new InitPayments()
    val keysFileOps = mock[KeysFileOps]

    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Igor/privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Igor/publicKey")).thenReturn("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==")
    // whether payment transaction could be created and signed
    when(keysFileOps.isKeysDirExists("John")).thenReturn(true)
    // needed to sign payment transaction by public key of local node's owner
    when(keysFileOps.readKeyFromFile("John/privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAimtA53n1kVMdG1OleLJtfbFnjr1zU5smd04yfbdWpUw==")
    when(keysFileOps.readKeyFromFile("John/publicKey")).thenReturn("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q==")

    val signedMessage = InitPaymentMessage.apply("Igor", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==",
      "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q==", Money("EUR", 2025), keysFileOps)
    val is = new ByteArrayInputStream(signedMessage.serialize.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler("John", mockBcHttpServer, initPayments, blockChain, keysFileOps).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Payment transaction created and added to blockchain."))

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

}
