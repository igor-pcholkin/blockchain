package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core._
import core.messages.{InitPaymentMessage, Message, NewBlockMessage, PaymentTransaction}
import keys.{KeysFileOps, KeysSerializator}
import util.StringConverter

import scala.io.Source
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import peers.PeerAccess
import util.HttpUtil.withHttpMethod
import io.circe.generic.auto._

class MsgHandler(nodeName: String, bcHttpServer: BCHttpServer, initPayments: InitPayments, bc: BlockChain, val keysFileOps: KeysFileOps,
                 peerAccess: PeerAccess) extends HttpHandler
  with StringConverter with KeysSerializator {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("POST", exchange, bcHttpServer) {
      val msgAsString = Source.fromInputStream(exchange.getRequestBody).getLines().mkString("\n")
      Message.deserialize(msgAsString).getOrElse {
        bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, s"Invalid message received: $msgAsString")
      } match {
        case initPaymentMessage: InitPaymentMessage =>
          if (verifySignature(initPaymentMessage)) {
            initPayments.add(initPaymentMessage)
            if (isLocalKey()) {
              addTransactionToNewBlock(initPaymentMessage)
              peerAccess.sendMsg(NewBlockMessage(bc.getLatestBlock))
              bcHttpServer.sendHttpResponse(exchange, "Payment transaction created and added to blockchain.")
            } else {
              bcHttpServer.sendHttpResponse(exchange, "Initial payment message verified and added to message cache.")
            }
          } else {
            bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "Initial payment message validation failed.")
          }
        case newBlockMessage: NewBlockMessage =>
          bc.add(newBlockMessage.block)
          bcHttpServer.sendHttpResponse(exchange, "New block received and added to blockchain.")
        case _ =>
          throw new RuntimeException(s"Unexpected message: $msgAsString")
      }
    }
  }

  def isLocalKey() = keysFileOps.isKeysDirExists(nodeName)

  def addTransactionToNewBlock(initPaymentMessage: InitPaymentMessage) = {
    val paymentTransaction = PaymentTransaction(nodeName, initPaymentMessage, keysFileOps)
    val serializedTransaction = Message.serialize(paymentTransaction).getBytes
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