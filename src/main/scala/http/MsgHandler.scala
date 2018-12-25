package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core._
import messages._
import keys.{KeysFileOps, KeysSerializator}
import util.{HttpUtil, StringConverter}

import scala.io.Source
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import peers.PeerAccess
import io.circe.generic.auto._

class MsgHandler(nodeName: String, bcHttpServer: BCHttpServer, initPayments: InitPayments, bc: BlockChain, val keysFileOps: KeysFileOps,
                 peerAccess: PeerAccess) extends HttpHandler with HttpUtil
  with StringConverter with KeysSerializator {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("POST", exchange, bcHttpServer) {
      val msgAsString = Source.fromInputStream(exchange.getRequestBody).getLines().mkString("\n")
      Message.deserialize(msgAsString).getOrElse {
        bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, s"Invalid message received: $msgAsString")
      } match {
        case initPaymentMessage: InitPaymentMessage =>
          handle(initPaymentMessage, exchange)
        case newBlockMessage: NewBlockMessage =>
          handle(newBlockMessage, exchange, peerAccess)
        case addPeersMessage: AddPeersMessage =>
          handle(addPeersMessage, exchange)
        case _ =>
          throw new RuntimeException(s"Unexpected message: $msgAsString")
      }
    }
  }

  private def handle(initPaymentMessage: InitPaymentMessage, exchange: HttpExchange): Unit = {
    if (verifySignature(initPaymentMessage)) {
      initPayments.add(initPaymentMessage)
      if (isLocalHostDestinationFor(initPaymentMessage)) {
        addTransactionToNewBlock(initPaymentMessage)
        peerAccess.sendMsg(NewBlockMessage(bc.getLatestBlock))
        bcHttpServer.sendHttpResponse(exchange, "Payment transaction created and added to blockchain.")
      } else {
        peerAccess.sendMsg(initPaymentMessage)
        bcHttpServer.sendHttpResponse(exchange, "Initial payment message verified and added to message cache.")
      }
    } else {
      bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "Initial payment message validation failed.")
    }
  }

  private def handle(newBlockMessage: NewBlockMessage, exchange: HttpExchange, peerAccess: PeerAccess): Unit = {
    bc.add(newBlockMessage.block)
    bc.writeChain()
    peerAccess.sendMsg(newBlockMessage)
    bcHttpServer.sendHttpResponse(exchange, "New block received and added to blockchain.")
  }

  private def handle(addPeersMessage: AddPeersMessage, exchange: HttpExchange): Unit = {
    peerAccess.addAll(addPeersMessage.peers)
    bcHttpServer.sendHttpResponse(exchange, "New peers received and added to the node.")
  }

  private def isLocalHostDestinationFor(initPaymentMessage: InitPaymentMessage): Boolean = {
    keysFileOps.getUserByKey(nodeName, initPaymentMessage.toPublicKeyEncoded).nonEmpty
  }

  def addTransactionToNewBlock(initPaymentMessage: InitPaymentMessage): Unit = {
    PaymentTransaction(nodeName, initPaymentMessage, keysFileOps) foreach { paymentTransaction =>
      val serializedTransaction = Message.serialize(paymentTransaction).getBytes
      val newBlock = bc.genNextBlock(serializedTransaction)
      bc.add(newBlock)
    }
  }

  def verifySignature(initPaymentMessage: InitPaymentMessage): Boolean = {
    initPaymentMessage.encodedSignature match {
      case Some(encodedSignature) =>
        val decodedSignature = base64StrToBytes(encodedSignature)
        val decodedPublicKey = deserializePublic(initPaymentMessage.fromPublicKeyEncoded)
        Signer.verify(decodedSignature, initPaymentMessage.dataToSign, decodedPublicKey)
      case None => false
    }
  }
}