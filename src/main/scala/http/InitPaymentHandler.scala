package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.messages.InitPaymentMessage
import core.{InitPayments, Money, Signer}
import keys.KeysFileOps
import peers.PeerAccess

import scala.io.Source
import io.circe.generic.auto._
import io.circe.parser._
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_CREATED}
import util.HttpUtil

case class InitPaymentRequest(from: String, to: String, currency: String, amount: Double)

class InitPaymentHandler(nodeName: String, bcHttpServer: BCHttpServer, initPayments: InitPayments, implicit val keysFileOps: KeysFileOps,
                         peerAccess: PeerAccess) extends HttpHandler with HttpUtil {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("POST", exchange, bcHttpServer) {
      val s = Source.fromInputStream(exchange.getRequestBody)
      val inputAsString = s.getLines.mkString
      s.close()
      decode[InitPaymentRequest](inputAsString) match {
        case Right(initPayment) =>
          validateFields (initPayment, exchange)
          val asset = Money (initPayment.currency, (BigDecimal (initPayment.amount) * 100).toLong)
          InitPaymentMessage.apply(nodeName, initPayment.from, initPayment.to, asset, keysFileOps) match {
            case Some(signedMessage) =>
              initPayments.add (signedMessage)
              peerAccess.sendMsg (signedMessage)
              bcHttpServer.sendHttpResponse (exchange, SC_CREATED, "New Payment has been initiated.")
            case None =>
              bcHttpServer.sendHttpResponse (exchange, SC_BAD_REQUEST, "No user with given (from) public key found.")
          }
        case Left(error) =>
          bcHttpServer.sendHttpResponse (exchange, SC_BAD_REQUEST, error.getMessage)
      }
    }
  }

  def validateFields(initPaymentRequest: InitPaymentRequest, exchange: HttpExchange) = {
    checkNonEmpty(initPaymentRequest.from, "from", exchange)
    checkNonEmpty(initPaymentRequest.to, "to", exchange)
    checkNonEmpty(initPaymentRequest.currency, "currency", exchange)
    checkNonEmpty(initPaymentRequest.amount, "amount", exchange)
  }

  def checkNonEmpty[T](field: T, fieldName: String, exchange: HttpExchange) = {
    if (Option(field).isEmpty) {
      bcHttpServer.sendHttpResponse(exchange, 400, s"$fieldName field in invoice is missing.")
    }
  }
}
