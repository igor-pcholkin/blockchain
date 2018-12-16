package http

import java.io.IOException
import java.time.LocalDateTime
import java.util.Currency

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.{InitPaymentMessage, InitPayments, Money, Signer}
import keys.KeysFileOps
import peers.PeerAccess

import scala.io.Source
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

case class InitPayment(from: String, to: String, currency: String, amount: Double)

class InitPaymentHandler(nodeName: String, bcHttpServer: BCHttpServer, initPayments: InitPayments, val keysFileOps: KeysFileOps,
                         peerAccess: PeerAccess) extends HttpHandler {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    if (exchange.getRequestMethod == "POST") {
      val s = Source.fromInputStream(exchange.getRequestBody)
      val inputAsString = s.getLines.mkString
      s.close()
      decode[InitPayment](inputAsString) match {
        case Right(initPayment) =>
          validateFields (initPayment, exchange)
          val asset = Money (initPayment.currency, (BigDecimal (initPayment.amount) * 100).toLong)
          val notSignedMessage = InitPaymentMessage (nodeName, initPayment.from, initPayment.to, asset, LocalDateTime.now)
          val signer = new Signer (keysFileOps)
          val initPaymentMessageSigned = notSignedMessage.copy (fromSignature = Some (signer.sign (nodeName, notSignedMessage) ) )
          initPayments.add (initPaymentMessageSigned)
          peerAccess.sendMsg (initPaymentMessageSigned)
          bcHttpServer.sendHttpResponse (exchange, 201, "New Payment has been initiated.")
        case Left(error) =>
          bcHttpServer.sendHttpResponse (exchange, 400, error.getMessage)
      }
    } else {
      bcHttpServer.sendHttpResponse(exchange, 400, "Invalid method, use POST")
    }
  }

  def validateFields(initPayment: InitPayment, exchange: HttpExchange) = {
    checkNonEmpty(initPayment.from, "from", exchange)
    checkNonEmpty(initPayment.to, "to", exchange)
    checkNonEmpty(initPayment.currency, "currency", exchange)
    checkNonEmpty(initPayment.amount, "amount", exchange)
  }

  def checkNonEmpty[T](field: T, fieldName: String, exchange: HttpExchange) = {
    if (Option(field).isEmpty) {
      bcHttpServer.sendHttpResponse(exchange, 400, s"$fieldName field in invoice is missing.")
    }
  }
}
