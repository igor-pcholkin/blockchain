package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.Currency

import com.sun.net.httpserver.HttpExchange
import core.{InitPayments, Money, Signer}
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import util.DateTimeUtil

class InitPaymentHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with DateTimeUtil {
  "InitPaymentHandler" should "initialize new payment to node and send it to another peers" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val initPayments = new InitPayments

    val initPayment =
      """From: A
        |To: B
        |Currency: EUR
        |Amount: 20.00
      """.stripMargin
    val is = new ByteArrayInputStream(initPayment.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    val nodeName = "Riga"
    new InitPaymentHandler(nodeName, mockBcHttpServer, initPayments).handle(mockExchange)

    initPayments.initPayments.size shouldBe 1
    val createdInitPayment = initPayments.initPayments.peek()
    createdInitPayment.createdBy shouldBe "Riga"
    createdInitPayment.from shouldBe "A"
    createdInitPayment.to shouldBe "B"
    createdInitPayment.asset shouldBe Money(Currency.getInstance("EUR"), 2000)
    timeStampsAreWithin(createdInitPayment.timestamp, LocalDateTime.now, 1000) shouldBe true
    Signer.verify(nodeName, createdInitPayment, createdInitPayment.fromSignature.getOrElse(Array[Byte]())) shouldBe true

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(201),
      Matchers.eq("New Payment has been initiated."))
  }
}
