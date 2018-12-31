package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import messages.{NewBlockMessage, SignedStatementMessage}
import core.{BlockChain, Money, StatementsCache}
import keys.KeysFileOps
import peers.PeerAccess

import scala.io.Source
import io.circe.generic.auto._
import io.circe.parser._
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_CREATED}
import statements.InitPayment
import util.HttpUtil

case class InitPaymentRequest(from: String, to: String, currency: String, amount: Double)

class InitPaymentHandler(nodeName: String, bcHttpServer: BCHttpServer, statementsCache: StatementsCache, implicit val keysFileOps: KeysFileOps,
                         peerAccess: PeerAccess, bc: BlockChain) extends HttpHandler with HttpUtil {
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
    InitPayment.apply(nodeName, initPaymentRequest.from, initPaymentRequest.to, asset,
      keysFileOps) match {
      case Right(initPayment) =>
        val signedStatement = SignedStatementMessage(initPayment, Seq(initPaymentRequest.from, initPaymentRequest.to), nodeName,
          keysFileOps, bcHttpServer.localServerAddress)
        processSignedStatement(signedStatement, initPaymentRequest.from, exchange)
      case Left(error) =>
        bcHttpServer.sendHttpResponse (exchange, SC_BAD_REQUEST, error)
    }
  }

  private def processSignedStatement(signedStatement: SignedStatementMessage, fromPublicKey: String, exchange: HttpExchange): Unit = {
    signedStatement.providedSignaturesForKeys.find(_._1 == fromPublicKey) match {
      case Some(_) =>
        if (signedStatement.isSignedByAllKeys) {
          createAndAddTransactionToBlockchain(signedStatement, exchange)
        } else {
          initiatePayment(signedStatement, exchange)
        }
      case None =>
        bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "No user with given (from) public key found.")
    }
  }

  private def createAndAddTransactionToBlockchain(signedStatement: SignedStatementMessage, exchange: HttpExchange): Unit = {
    bc.addFactToNewBlock(signedStatement)
    peerAccess.sendMsg(NewBlockMessage(bc.getLatestBlock, bcHttpServer.localServerAddress))
    bcHttpServer.sendHttpResponse(exchange, "Payment transaction created and added to blockchain.")
  }

  private def initiatePayment(signedStatement: SignedStatementMessage, exchange: HttpExchange): Unit = {
    statementsCache.add(signedStatement)
    peerAccess.sendMsg(signedStatement)(SignedStatementMessage.encoder)
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
