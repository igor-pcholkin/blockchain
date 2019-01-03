package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core._
import io.circe.Encoder
import messages._
import keys.{KeysFileOps, KeysSerializator}
import util.{HttpUtil, StringConverter}

import scala.io.Source
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import peers.PeerAccess
import io.circe.generic.auto._

import scala.collection.JavaConverters._

class MsgHandler(nodeName: String, bcHttpServer: BCHttpServer, statementsCache: StatementsCache, bc: BlockChain, val keysFileOps: KeysFileOps,
                 peerAccess: PeerAccess) extends HttpHandler with HttpUtil
  with StringConverter with KeysSerializator {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("POST", exchange, bcHttpServer) {
      val msgAsString = Source.fromInputStream(exchange.getRequestBody).getLines().mkString("\n")
      val msg = Serializator.deserialize(msgAsString).getOrElse {
        bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, s"Invalid message received: $msgAsString")
      } match {
        case signedStatement: SignedStatementMessage =>
          handle(signedStatement, exchange)
        case newBlockMessage: NewBlockMessage =>
          handle(newBlockMessage, exchange, peerAccess)
        case addPeersMessage: AddPeersMessage =>
          handle(addPeersMessage, exchange)
        case requestAllMessages: RequestAllStatementsMessage =>
          handle(requestAllMessages, exchange)
        case requestBlocksMessage: RequestBlocksMessage =>
          handle(requestBlocksMessage, exchange)
        case _ =>
          throw new RuntimeException(s"Unexpected message: $msgAsString")
      }
      peerAccess.add(msg.sentFromIPAddress)
    }
  }

  private def handle(signedStatement: SignedStatementMessage, exchange: HttpExchange): Message = {
    if (verifySignatures(signedStatement)) {
      val newSignatures = signedStatement.signByLocalPublicKeys(nodeName, keysFileOps)
      val enhancedStatement = signedStatement.addSignatures(newSignatures)
      statementsCache.add(enhancedStatement)
      if (enhancedStatement.isSignedByAllKeys) {
        bc.addFactToNewBlock(enhancedStatement)
        peerAccess.sendMsg(NewBlockMessage(bc.getLatestBlock, bc.chain.size() - 1, peerAccess.localHost.localServerAddress))
        bcHttpServer.sendHttpResponse(exchange, "Payment transaction created and added to blockchain.")
      } else {
        peerAccess.sendMsg(signedStatement)(SignedStatementMessage.encoder)
        bcHttpServer.sendHttpResponse(exchange, "Statement has been verified and added to cache.")
      }
    } else {
      bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "Initial payment message validation failed.")
    }
    signedStatement
  }

  private def handle(newBlockMessage: NewBlockMessage, exchange: HttpExchange, peerAccess: PeerAccess): Message = {
    bc.add(newBlockMessage.block)
    bc.writeChain()
    peerAccess.sendMsg(newBlockMessage)
    bcHttpServer.sendHttpResponse(exchange, "New block received and added to blockchain.")
    newBlockMessage
  }

  private def handle(addPeersMessage: AddPeersMessage, exchange: HttpExchange): Message = {
    peerAccess.addAll(addPeersMessage.peers)
    bcHttpServer.sendHttpResponse(exchange, "New peers received and added to the node.")
    addPeersMessage
  }

  private def handle(requestAllStatementsMessage: RequestAllStatementsMessage, exchange: HttpExchange): Message = {
    statementsCache.statements.values.asScala foreach { statement =>
      peerAccess.sendMsg(statement, requestAllStatementsMessage.sentFromIPAddress)(SignedStatementMessage.encoder)
    }
    bcHttpServer.sendHttpResponse(exchange, s"All statements have been sent to node: ${requestAllStatementsMessage.sentFromIPAddress}.")
    requestAllStatementsMessage
  }

  private def handle(requestBlocksMessage: RequestBlocksMessage, exchange: HttpExchange): Message = {
    bc.chain.iterator().asScala.drop(requestBlocksMessage.fromBlockNo).zipWithIndex foreach { case (block, i) =>
      peerAccess.sendMsg(NewBlockMessage(block, requestBlocksMessage.fromBlockNo + i, peerAccess.localHost.localServerAddress),
        requestBlocksMessage.sentFromIPAddress)(Encoder[NewBlockMessage])
    }
    bcHttpServer.sendHttpResponse(exchange, s"All requested blocks have been sent to node: ${requestBlocksMessage.sentFromIPAddress}.")
    requestBlocksMessage
  }

  def verifySignatures(signedStatement: SignedStatementMessage): Boolean = {
    signedStatement.providedSignaturesForKeys.forall {
      case (encodedPublicKey, signature) => verifySignature(signedStatement, encodedPublicKey, signature)
    }
  }

  def verifySignature(signedStatement: SignedStatementMessage, publicKeyEncoded: String, encodedSignature: String): Boolean = {
    val decodedSignature = base64StrToBytes(encodedSignature)
    val decodedPublicKey = deserializePublic(publicKeyEncoded)
    Signer.verify(decodedSignature, signedStatement.statement.dataToSign, decodedPublicKey)
  }

}