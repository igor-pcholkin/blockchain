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

import json.MessageEnvelopeJson.deserialize

class MsgHandler(nodeName: String, override val bcHttpServer: BCHttpServer, override val statementsCache: StatementsCache,
                 override val bc: BlockChain, val keysFileOps: KeysFileOps,
                 override val peerAccess: PeerAccess) extends HttpHandler with HttpUtil with MsgHandlerOps
  with StringConverter with KeysSerializator {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("PUT", exchange, bcHttpServer) {
      val msgAsString = Source.fromInputStream(exchange.getRequestBody).getLines().mkString("\n")
      deserialize(msgAsString) match {
        case Right(me) =>
          me.message match {
            case signedStatement: SignedStatementMessage =>
              handle(signedStatement, exchange)
            case newBlockMessage: NewBlockMessage =>
              handle(newBlockMessage, exchange)
            case addPeersMessage: AddPeersMessage =>
              handle(addPeersMessage, exchange)
            case pullNewsMessage: PullNewsMessage =>
              handle(pullNewsMessage, exchange, me.sentFromIPAddress)
            case _ =>
              throw new RuntimeException(s"Unexpected message: $msgAsString")
          }
          peerAccess.add(me.sentFromIPAddress)
        case Left(error) => bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, s"Invalid message envelope received: ${error.getMessage}")
      }
    }
  }

  private def handle(signedStatement: SignedStatementMessage, exchange: HttpExchange): Unit = {
    if (statementsCache.contains(signedStatement)) {
      bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "The statement has been received before.")
    } else if (bc.containsFactInside(signedStatement.statement)) {
      bcHttpServer.sendHttpResponse(exchange, "The statement refused: blockchain alrady contains it as a fact.")
    } else if (verifySignatures(signedStatement)) {
      val newSignatures = signedStatement.signByLocalPublicKeys(nodeName, keysFileOps)
      val enhancedStatement = signedStatement.addSignatures(newSignatures)
      statementsCache.add(enhancedStatement)
      if (enhancedStatement.isSignedByAllKeys) {
        processStatementAsFact(enhancedStatement, exchange)
      } else {
        peerAccess.sendMsg(signedStatement)
        bcHttpServer.sendHttpResponse(exchange, "Statement has been verified and added to cache.")
      }
    } else {
      bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "Initial payment message validation failed.")
    }
  }

  private def handle(newBlockMessage: NewBlockMessage, exchange: HttpExchange): Unit = {
    if (bc.containsFactInside(newBlockMessage.block)) {
      bcHttpServer.sendHttpResponse(exchange, "New block refused - contains existing fact.")
    } else {
      val newBlock = newBlockMessage.block
      bc.extractFact(newBlock) match {
        case Right(fact) =>
          if (bc.isValid(newBlock)) {
            appendBlock(newBlockMessage, fact.statement, exchange)
          } else if (bc.isValid(newBlock, newBlockMessage.blockNo)) {
            if (newBlock.isNewerThan(bc.getLatestBlock)) {
              appendBlockWithAdjustedIndex(newBlockMessage, fact.statement, exchange)
            } else {
              replaceChainTailWithNewBlock(newBlockMessage, fact.statement, exchange)
            }
          } else {
            bcHttpServer.sendHttpResponse(exchange, "Invalid block received - rejected.")
          }
        case Left(_) =>
          bcHttpServer.sendHttpResponse(exchange, "Invalid block received - rejected.")
      }
    }
  }

  private def appendBlock(newBlockMessage: NewBlockMessage, statement: Statement, exchange: HttpExchange): Unit = {
    bc.add(newBlockMessage.block)
    bc.writeChain()
    statementsCache.remove(statement)
    peerAccess.sendMsg(newBlockMessage)
    bcHttpServer.sendHttpResponse(exchange, "New block received and added to blockchain.")
  }

  private def appendBlockWithAdjustedIndex(newBlockMessage: NewBlockMessage, statement: Statement, exchange: HttpExchange): Unit = {
    val newBlockRegenerated = bc.genNextBlock(newBlockMessage.block.data)
    bc.add(newBlockRegenerated)
    bc.writeChain()
    statementsCache.remove(statement)
    newBlockMessage.blockNo until bc.size foreach { i =>
      peerAccess.sendMsg(NewBlockMessage(bc.blockAt(i), i))
    }
    bcHttpServer.sendHttpResponse(exchange, "New block received and added to blockchain with adjusted index.")
  }

  private def replaceChainTailWithNewBlock(newBlockMessage: NewBlockMessage, statement: Statement, exchange: HttpExchange): Unit = {
    val blocksToResend = bc.blocksFrom(newBlockMessage.blockNo)
    bc.deleteChainFrom(newBlockMessage.blockNo)
    bc.takeN(newBlockMessage.blockNo)
    bc.add(newBlockMessage.block)
    bc.writeChain()
    statementsCache.remove(statement)
    blocksToResend.prepend(newBlockMessage.block)
    blocksToResend.zipWithIndex foreach { case (block, i) =>
      peerAccess.sendMsg(NewBlockMessage(block, newBlockMessage.blockNo + i))
    }
    bcHttpServer.sendHttpResponse(exchange, "New block received and inserted to blockchain adjusting existing blocks.")
  }

  private def handle(addPeersMessage: AddPeersMessage, exchange: HttpExchange): Unit = {
    peerAccess.addAll(addPeersMessage.peers)
    bcHttpServer.sendHttpResponse(exchange, "New peers received and added to the node.")
  }

  private def handle(pullNewsMessage: PullNewsMessage, exchange: HttpExchange, sentFromIPAddress: String): Unit = {
    sendAllStatementsToPeers(sentFromIPAddress)
    sendAllBlocksToPeers(sentFromIPAddress, pullNewsMessage.fromBlockNo)
    if (!pullNewsMessage.inReply) {
      peerAccess.sendMsg(PullNewsMessage(bc.size, inReply = true), sentFromIPAddress)
    }
    bcHttpServer.sendHttpResponse(exchange, s"All statements and blocks have been sent to node: $sentFromIPAddress.")
  }

  private def sendAllStatementsToPeers(sentFromIPAddress: String): Unit = {
    statementsCache.allStatementMessages foreach { statement =>
      peerAccess.sendMsg(statement, sentFromIPAddress)
    }
  }

  private def sendAllBlocksToPeers(sentFromIPAddress: String, fromBlockNo: Int): Unit = {
    bc.blocksFrom(fromBlockNo).zipWithIndex foreach { case (block, i) =>
      peerAccess.sendMsg(NewBlockMessage(block, fromBlockNo + i), sentFromIPAddress)
    }
  }

  private def verifySignatures(signedStatement: SignedStatementMessage): Boolean = {
    signedStatement.providedSignaturesForKeys.forall {
      case (encodedPublicKey, signature) => verifySignature(signedStatement, encodedPublicKey, signature)
    }
  }

  private def verifySignature(signedStatement: SignedStatementMessage, publicKeyEncoded: String, encodedSignature: String): Boolean = {
    val decodedSignature = base64StrToBytes(encodedSignature)
    val decodedPublicKey = deserializePublic(publicKeyEncoded)
    Signer.verify(decodedSignature, signedStatement.statement.dataToSign, decodedPublicKey)
  }

}