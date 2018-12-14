package http

import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.util.Currency

import com.sun.net.httpserver.HttpExchange
import core.{Invoices, Money}
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import util.DateTimeUtil

class AddInvoiceHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with DateTimeUtil {
  "AddInvoiceHandler" should "add invoice to node and send it to another peers" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val invoices = new Invoices

    val invoice =
      """From: A
        |To: B
        |Currency: EUR
        |Amount: 20.00
      """.stripMargin
    val is = new ByteArrayInputStream(invoice.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new AddInvoiceHandler("Riga", mockBcHttpServer, invoices).handle(mockExchange)

    invoices.invoices.size shouldBe 1
    val createdInvoice = invoices.invoices.peek()
    createdInvoice.createdBy shouldBe "Riga"
    createdInvoice.from shouldBe "A"
    createdInvoice.to shouldBe "B"
    createdInvoice.asset shouldBe Money(Currency.getInstance("EUR"), 2000)
    timeStampsAreWithin(createdInvoice.timestamp, LocalDateTime.now, 1000) shouldBe true

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(201),
      Matchers.eq("New invoice has been added."))
  }
}
