package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import messages.InitPaymentMessage
import core.{Statements, Money}
import keys.KeysFileOps
import peers.PeerAccess

import scala.io.Source
import io.circe.generic.auto._
import io.circe.parser._
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_CREATED}
import util.HttpUtil

case class InitPaymentRequest(from: String, to: String, currency: String, amount: Double)

class InitPaymentHandler(nodeName: String, bcHttpServer: BCHttpServer, statements: Statements, implicit val keysFileOps: KeysFileOps,
                         peerAccess: PeerAccess) extends HttpHandler with HttpUtil {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("POST", exchange, bcHttpServer) {
      val s = Source.fromInputStream(exchange.getRequestBody)
      val inputAsString = s.getLines.mkString
      s.close()
      decode[InitPaymentRequest](inputAsString) match {
        case Right(initPayment) =>
          val asset = Money (initPayment.currency, (BigDecimal (initPayment.amount) * 100).toLong)
          InitPaymentMessage.apply(nodeName, initPayment.from, initPayment.to, asset, keysFileOps) match {
            case Right(signedMessage) =>
              statements.add (signedMessage)
              peerAccess.sendMsg (signedMessage)
              bcHttpServer.sendHttpResponse (exchange, SC_CREATED, "New Payment has been initiated.")
            case Left(error) =>
              bcHttpServer.sendHttpResponse (exchange, SC_BAD_REQUEST, error)
          }
        case Left(error) =>
          val correctedMessage = correctValidationError(exchange, error.getMessage) match {
            case Some(message) => message
            case None => error.getMessage
          }
          bcHttpServer.sendHttpResponse (exchange, SC_BAD_REQUEST, correctedMessage)
      }
    }
  }

  private def correctValidationError(exchange: HttpExchange, error: String) = {
    Stream("from", "to", "currency", "amount").flatMap {
      checkField(_, error, exchange)
    }.find(_.nonEmpty)
  }

  private def checkField(fieldName: String, error: String, exchange: HttpExchange) = {
    if (error == s"Attempt to decode value on failed cursor: DownField($fieldName)") {
      Some(s""""$fieldName" field in payment request is missing.""")
    } else
      None
  }
}
