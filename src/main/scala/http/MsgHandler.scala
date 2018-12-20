package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core._
import keys.{KeysFileOps, KeysSerializator}
import util.StringConverter

import scala.io.Source
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import util.HttpUtil.withHttpMethod

class MsgHandler(nodeName: String, bcHttpServer: BCHttpServer, initPayments: InitPayments, bc: BlockChain, val keysFileOps: KeysFileOps) extends HttpHandler
  with StringConverter with KeysSerializator {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("POST", exchange, bcHttpServer) {
      val msgAsString = Source.fromInputStream(exchange.getRequestBody).getLines().mkString("\n")
      val message = InitPaymentMessage.deserialize(msgAsString) match {
        case Right(initPaymentMessage) =>
          if (verifySignature(initPaymentMessage)) {
            initPayments.add(initPaymentMessage)
            if (isLocalKey()) {
              addTransactionToNewBlock(initPaymentMessage)
              bcHttpServer.sendHttpResponse(exchange, "Payment transaction created and added to blockchain.")
            } else {
              bcHttpServer.sendHttpResponse(exchange, "Initial payment message verified and added to message cache.")
            }
          } else {
            bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "Initial payment message validation failed.")
          }
        case Left(error) =>
          bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, error.getMessage)

      }
    }
  }

  def isLocalKey() = keysFileOps.isKeysDirExists(nodeName)

  def addTransactionToNewBlock(initPaymentMessage: InitPaymentMessage) = {
    val paymentTransaction = PaymentTransaction(nodeName, initPaymentMessage, keysFileOps)
    val serializedTransaction = paymentTransaction.serialize.getBytes
    val newBlock = bc.genNextBlock(serializedTransaction)
    bc.add(newBlock)
  }

  def verifySignature(initPaymentMessage: InitPaymentMessage) = {
    initPaymentMessage.encodedSignature match {
      case Some(encodedSignature) =>
        val decodedSignature = base64StrToBytes(encodedSignature)
        val decodedPublicKey = deserializePublic(initPaymentMessage.fromPublicKeyEncoded)
        Signer.verify(decodedSignature, initPaymentMessage.dataToSign, decodedPublicKey)
      case None => false
    }
  }
}