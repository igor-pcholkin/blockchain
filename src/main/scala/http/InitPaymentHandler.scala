package http

import java.io.IOException

import business.Money
import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import messages.SignedStatementMessage

import scala.io.Source
import io.circe.generic.auto._
import io.circe.parser._
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_CREATED}
import statements.Payment
import util.HttpUtil

case class InitPaymentRequest(from: String, to: String, currency: String, amount: Double)

class InitPaymentHandler(hc: HttpContext) extends HttpHandler with HttpUtil with MsgHandlerOps {

  override val blockChain = hc.blockChain
  override val bcHttpServer = hc.bcHttpServer
  override val statementsCache = hc.statementsCache
  override val peerAccess = hc.peerAccess

  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("POST", exchange, bcHttpServer) {
      val s = Source.fromInputStream(exchange.getRequestBody)
      val inputAsString = s.getLines.mkString
      s.close()
      decode[InitPaymentRequest](inputAsString) match {
        case Right(initPaymentRequest) =>
          processPaymentRequest(initPaymentRequest, exchange)
        case Left(error) =>
          val correctedMessage = correctValidationError(exchange, error.getMessage) match {
            case Some(message) => message
            case None => error.getMessage
          }
          bcHttpServer.sendHttpResponse (exchange, SC_BAD_REQUEST, correctedMessage)
      }
    }
  }

  private def processPaymentRequest(initPaymentRequest: InitPaymentRequest, exchange: HttpExchange): Unit = {
    val asset = Money (initPaymentRequest.currency, (BigDecimal (initPaymentRequest.amount) * 100).toLong)
    Payment.verifyAndCreate(hc.nodeName, initPaymentRequest.from, initPaymentRequest.to, asset) match {
      case Right(payment) =>
        val signedStatement = SignedStatementMessage(payment,
          Seq(initPaymentRequest.from, initPaymentRequest.to), hc.nodeName, hc.keysFileOps)
        processSignedStatement(signedStatement, initPaymentRequest.from, exchange)
      case Left(error) =>
        bcHttpServer.sendHttpResponse (exchange, SC_BAD_REQUEST, error)
    }
  }

  private def processSignedStatement(signedStatement: SignedStatementMessage,
    fromPublicKey: String, exchange: HttpExchange): Unit = {
    signedStatement.providedSignaturesForKeys.find(_._1 == fromPublicKey) match {
      case Some(_) =>
        if (signedStatement.isSignedByAllKeys) {
          processStatementAsFact(signedStatement, exchange)
        } else {
          initiatePayment(signedStatement, exchange)
        }
      case None =>
        bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "No user with given (from) public key found.")
    }
  }

  private def initiatePayment(signedStatement: SignedStatementMessage, exchange: HttpExchange): Unit = {
    statementsCache.add(signedStatement)
    peerAccess.sendMsg(signedStatement)
    bcHttpServer.sendHttpResponse(exchange, SC_CREATED, "New Payment has been initiated.")
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
