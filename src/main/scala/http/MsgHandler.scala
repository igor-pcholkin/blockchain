package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core._
import messages._
import keys.KeysSerializator
import util.{HttpUtil, StringConverter}

import scala.io.Source
import org.apache.http.HttpStatus.SC_BAD_REQUEST

import json.MessageEnvelopeJson.deserialize

class MsgHandler(hc: HttpContext) extends HttpHandler with HttpUtil with MsgHandlerOps
  with StringConverter with KeysSerializator {

  override val blockChain = hc.blockChain
  override val bcHttpServer = hc.bcHttpServer
  override val statementsCache = hc.statementsCache
  override val peerAccess = hc.peerAccess
  override val keysFileOps = hc.keysFileOps

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
        case Left(error) => bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST,
          s"Invalid message envelope received: ${error.getMessage}")
      }
    }
  }

  private def handle(signedStatement: SignedStatementMessage, exchange: HttpExchange): Unit = {
    if (statementsCache.contains(signedStatement)) {
      bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "The statement has been received before.")
    } else if (blockChain.containsFactInside(signedStatement.statement)) {
      bcHttpServer.sendHttpResponse(exchange, "The statement refused: blockchain alrady contains it as a fact.")
    } else if (signedStatement.verifySignatures(this)) {
      val newSignatures = signedStatement.signByLocalPublicKeys(hc.nodeName, keysFileOps)
      val enhancedStatement = signedStatement.addSignatures(newSignatures)
      statementsCache.add(enhancedStatement)
      if (enhancedStatement.isSignedByAllKeys) {
        processStatementAsFact(enhancedStatement, exchange)
      } else {
        peerAccess.sendMsg(signedStatement)
        bcHttpServer.sendHttpResponse(exchange, "Statement has been verified and added to cache.")
      }
    } else {
      bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "Statement validation failed.")
    }
  }

  private def handle(newBlockMessage: NewBlockMessage, exchange: HttpExchange): Unit = {
    if (blockChain.containsFactInside(newBlockMessage.block)) {
      bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "New block refused - contains existing fact.")
    } else {
      val newBlock = newBlockMessage.block
      blockChain.extractFact(newBlock) match {
        case Right(fact) =>
          if (!fact.verifySignatures(this)) {
            bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "Verification of signatures failed for fact in received block.")
          } else if (blockChain.isValid(newBlock)) {
            appendBlock(newBlockMessage, fact.statement, exchange)
          } else if (blockChain.isValid(newBlock, newBlockMessage.blockNo)) {
            if (newBlock.isNewerThan(blockChain.getLatestBlock)) {
              appendBlockWithAdjustedIndex(newBlockMessage, fact.statement, exchange)
            } else {
              replaceChainTailWithNewBlock(newBlockMessage, fact.statement, exchange)
            }
          } else {
            bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "Invalid block received - rejected.")
          }
        case Left(_) =>
          bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "Invalid block received - rejected.")
      }
    }
  }

  private def appendBlock(newBlockMessage: NewBlockMessage, statement: Statement, exchange: HttpExchange): Unit = {
    blockChain.add(newBlockMessage.block)
    blockChain.writeChain()
    statementsCache.remove(statement)
    peerAccess.sendMsg(newBlockMessage)
    bcHttpServer.sendHttpResponse(exchange, "New block received and added to blockchain.")
  }

  private def appendBlockWithAdjustedIndex(newBlockMessage: NewBlockMessage, statement: Statement,
                                           exchange: HttpExchange): Unit = {
    val newBlockRegenerated = blockChain.genNextBlock(newBlockMessage.block.data)
    blockChain.add(newBlockRegenerated)
    blockChain.writeChain()
    statementsCache.remove(statement)
    newBlockMessage.blockNo until blockChain.size foreach { i =>
      peerAccess.sendMsg(NewBlockMessage(blockChain.blockAt(i), i))
    }
    bcHttpServer.sendHttpResponse(exchange, "New block received and added to blockchain with adjusted index.")
  }

  private def replaceChainTailWithNewBlock(newBlockMessage: NewBlockMessage, statement: Statement,
                                           exchange: HttpExchange): Unit = {
    val blocksToResend = blockChain.blocksFrom(newBlockMessage.blockNo)
    blockChain.deleteChainFrom(newBlockMessage.blockNo)
    blockChain.takeN(newBlockMessage.blockNo)
    blockChain.add(newBlockMessage.block)
    blockChain.writeChain()
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
      peerAccess.sendMsg(PullNewsMessage(blockChain.size, inReply = true), sentFromIPAddress)
    }
    bcHttpServer.sendHttpResponse(exchange, s"All statements and blocks have been sent to node: $sentFromIPAddress.")
  }

  private def sendAllStatementsToPeers(sentFromIPAddress: String): Unit = {
    statementsCache.allStatementMessages foreach { statement =>
      peerAccess.sendMsg(statement, sentFromIPAddress)
    }
  }

  private def sendAllBlocksToPeers(sentFromIPAddress: String, fromBlockNo: Int): Unit = {
    blockChain.blocksFrom(fromBlockNo).zipWithIndex foreach { case (block, i) =>
      peerAccess.sendMsg(NewBlockMessage(block, fromBlockNo + i), sentFromIPAddress)
    }
  }

}