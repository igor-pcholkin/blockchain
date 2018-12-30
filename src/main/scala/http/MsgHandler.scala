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

class MsgHandler(nodeName: String, bcHttpServer: BCHttpServer, statementsCache: StatementsCache, bc: BlockChain, val keysFileOps: KeysFileOps,
                 peerAccess: PeerAccess) extends HttpHandler with HttpUtil
  with StringConverter with KeysSerializator {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("POST", exchange, bcHttpServer) {
      val msgAsString = Source.fromInputStream(exchange.getRequestBody).getLines().mkString("\n")
      Message.deserialize(msgAsString).getOrElse {
        bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, s"Invalid message received: $msgAsString")
      } match {
        case signedStatement: SignedStatement =>
          handle(signedStatement, exchange)
        case newBlockMessage: NewBlockMessage =>
          handle(newBlockMessage, exchange, peerAccess)
        case addPeersMessage: AddPeersMessage =>
          handle(addPeersMessage, exchange)
        case _ =>
          throw new RuntimeException(s"Unexpected message: $msgAsString")
      }
    }
  }

  private def handle(signedStatement: SignedStatement, exchange: HttpExchange): Unit = {
    if (verifySignatures(signedStatement)) {
      val newSignatures = signedStatement.signByLocalPublicKeys(nodeName, keysFileOps)
      val enhancedStatement = signedStatement.addSignatures(newSignatures)
      statementsCache.add(enhancedStatement)
      if (enhancedStatement.isSignedByAllKeys) {
        bc.addFactToNewBlock(enhancedStatement)
        peerAccess.sendMsg(NewBlockMessage(bc.getLatestBlock))
        bcHttpServer.sendHttpResponse(exchange, "Payment transaction created and added to blockchain.")
      } else {
        peerAccess.sendMsg(signedStatement)(SignedStatement.encoder)
        bcHttpServer.sendHttpResponse(exchange, "Statement has been verified and added to cache.")
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

  def verifySignatures(signedStatement: SignedStatement): Boolean = {
    signedStatement.providedSignaturesForKeys.forall {
      case (encodedPublicKey, signature) => verifySignature(signedStatement, encodedPublicKey, signature)
    }
  }

  def verifySignature(signedStatement: SignedStatement, publicKeyEncoded: String, encodedSignature: String): Boolean = {
    val decodedSignature = base64StrToBytes(encodedSignature)
    val decodedPublicKey = deserializePublic(publicKeyEncoded)
    Signer.verify(decodedSignature, signedStatement.statement.dataToSign, decodedPublicKey)
  }

}