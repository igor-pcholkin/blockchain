package http

import com.sun.net.httpserver.HttpExchange
import core.{BlockChain, StatementsCache}
import messages.{NewBlockMessage, SignedStatementMessage}
import peers.PeerAccess

trait MsgHandlerOps {

  val bcHttpServer: BCHttpServer
  val peerAccess: PeerAccess
  val blockChain: BlockChain
  val statementsCache: StatementsCache

  def processStatementAsFact(signedStatement: SignedStatementMessage, exchange: HttpExchange): Unit = {
    if (blockChain.containsFactInside(signedStatement.statement)) {
      bcHttpServer.sendHttpResponse(exchange, "Refused new block creation - existing fact.")
    } else {
      blockChain.addFactToNewBlock(signedStatement)
      statementsCache.remove(signedStatement.statement)
      peerAccess.sendMsg(NewBlockMessage(blockChain.getLatestBlock, blockChain.size - 1))
      bcHttpServer.sendHttpResponse(exchange, "New fact has been created and added to blockchain.")
    }
  }
}
