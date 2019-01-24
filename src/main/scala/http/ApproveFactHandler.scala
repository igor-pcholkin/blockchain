package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import messages.SignedStatementMessage
import keys.{KeysGenerator, KeysSerializator}

import scala.io.Source
import io.circe.generic.auto._
import io.circe.parser._
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import statements.ApprovedFact
import util.{HttpUtil, StringConverter}

case class ApproveFactRequest(factHash: String, approverUserName: String)

class ApproveFactHandler(hc: HttpContext)
  extends HttpHandler with HttpUtil with KeysGenerator with KeysSerializator with MsgHandlerOps with StringConverter {

  override val blockChain = hc.blockChain
  override val bcHttpServer = hc.bcHttpServer
  override val keysFileOps = hc.keysFileOps
  override val statementsCache = hc.statementsCache
  override val peerAccess = hc.peerAccess

  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("PUT", exchange, bcHttpServer) {
      val s = Source.fromInputStream(exchange.getRequestBody)
      val inputAsString = s.getLines.mkString
      s.close()
      decode[ApproveFactRequest](inputAsString) match {
        case Right(approveFactRequest) =>
          approveFact(approveFactRequest, exchange)
        case Left(error) =>
          val correctedMessage = correctValidationError(exchange, error.getMessage) match {
            case Some(message) => message
            case None => error.getMessage
          }
          bcHttpServer.sendHttpResponse (exchange, SC_BAD_REQUEST, correctedMessage)
      }
    }
  }

  private def approveFact(approveFactRequest: ApproveFactRequest, exchange: HttpExchange): Unit = {
    if (!keysFileOps.isKeysDirExists(hc.nodeName, approveFactRequest.approverUserName)) {
      bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST,
        s"User ${approveFactRequest.approverUserName} doesn't exist")
    } else {
      val approverPublicKey = readPublicKey(hc.nodeName, approveFactRequest.approverUserName)
      findFact(approveFactRequest.factHash) match {
        case Some(block) =>
          val statement = ApprovedFact(block.factHash, approverPublicKey)
          val signedStatement = SignedStatementMessage(statement, Seq(approverPublicKey), hc.nodeName, keysFileOps)
          processStatementAsFact(signedStatement, exchange)
        case None =>
          bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, s"Fact with given hash doesn't exist")
      }
    }
  }

  private def findFact(factHash: String) = blockChain.blocksFrom(0).find(_.factHash == factHash)

  private def correctValidationError(exchange: HttpExchange, error: String) = {
    Stream("factHash", "approverUserName").flatMap {
      checkField(_, error, exchange)
    }.find(_.nonEmpty)
  }

  private def checkField(fieldName: String, error: String, exchange: HttpExchange) = {
    if (error == s"Attempt to decode value on failed cursor: DownField($fieldName)") {
      Some(s""""$fieldName" field in approve request is missing.""")
    } else
      None
  }
}
