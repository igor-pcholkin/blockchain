package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime

import com.sun.net.httpserver.HttpExchange
import core.{InitPaymentMessage, InitPayments, Money}
import keys.KeysFileOps
import org.apache.http.HttpStatus
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar

class MsgHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar {

  "Message handler" should "verify and add initial payment message to message cache" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val initPayments = new InitPayments()
    val keysFileOps = mock[KeysFileOps]

    when(keysFileOps.readKeyFromFile("Igor/privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Igor/publicKey")).thenReturn("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==")

    val signedMessage = InitPaymentMessage.apply("Igor", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==",
      "(publicKeyTo)", Money("EUR", 2025), LocalDateTime.now, keysFileOps)
    val is = new ByteArrayInputStream(signedMessage.serialize.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler(mockBcHttpServer, initPayments, keysFileOps).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Initial payment message verified."))

    initPayments.initPayments.containsValue(signedMessage) shouldBe true
  }

  "Message handler" should "reject payment message during failed verification" in {

    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val initPayments = new InitPayments()
    val keysFileOps = mock[KeysFileOps]

    when(keysFileOps.readKeyFromFile("Igor/privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Igor/publicKey")).thenReturn("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==")

    val signedMessage = InitPaymentMessage.apply("Igor", "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==",
      "(publicKeyTo)", Money("EUR", 2025), LocalDateTime.now, keysFileOps)
    val tamperedMessage = signedMessage.copy(money = Money("EUR", 202500))
    val is = new ByteArrayInputStream(tamperedMessage.serialize.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new MsgHandler(mockBcHttpServer, initPayments, keysFileOps).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_BAD_REQUEST),
      Matchers.eq("Initial payment message validation failed."))

    initPayments.initPayments.containsValue(signedMessage) shouldBe false
  }

}
