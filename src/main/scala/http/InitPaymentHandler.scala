package http

import java.io.IOException
import java.time.LocalDateTime
import java.util.Currency

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.{InitPayment, InitPayments, Money, Signer}
import keys.KeysFileOps

import scala.io.Source

class InitPaymentHandler(nodeName: String, bcHttpServer: BCHttpServer, initPayments: InitPayments, val keysFileOps: KeysFileOps) extends HttpHandler {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    if (exchange.getRequestMethod == "POST") {
      val s = Source.fromInputStream(exchange.getRequestBody)
      val (from, to, currencyCode, amount) = s.getLines.foldLeft(("", "", "", "")) { (acc, line) =>
      val fields = line.split(":")
      if (fields.length > 1) {
        val value = fields(1).trim
        if (line.startsWith("From:")) {
          acc.copy(_1 = value)
        } else if (line.startsWith("To:")) {
          acc.copy(_2 = value)
        } else if (line.startsWith("Currency:")) {
          acc.copy(_3 = value)
        } else if (line.startsWith("Amount:")) {
          acc.copy(_4 = value)
        } else
          acc
      } else
        acc
      }
      s.close()
      checkNonEmpty(from, "From", exchange)
      checkNonEmpty(to, "To", exchange)
      checkNonEmpty(currencyCode, "Currency", exchange)
      checkNonEmpty(amount, "Amount", exchange)
      if (from.nonEmpty && to.nonEmpty && currencyCode.nonEmpty && amount.nonEmpty) {
        val asset = Money(Currency.getInstance(currencyCode), (BigDecimal(amount) * 100).toLong)
        val notSigned = InitPayment(nodeName, from, to, asset, LocalDateTime.now )
        val signer = new Signer(keysFileOps)
        val initPaymentSigned = notSigned.copy( fromSignature = Some(signer.sign(nodeName, notSigned)))
        initPayments.add(initPaymentSigned)
        bcHttpServer.sendHttpResponse(exchange, 201, "New Payment has been initiated.")
      }
    } else {
      bcHttpServer.sendHttpResponse(exchange, 400, "Invalid method, use POST")
    }
  }

  def checkNonEmpty(field: String, fieldName: String, exchange: HttpExchange) = {
    if (field.isEmpty) {
      bcHttpServer.sendHttpResponse(exchange, 400, s"$fieldName field in invoice is missing.")
    }
  }
}
