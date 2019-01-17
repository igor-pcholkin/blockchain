package http

import com.sun.net.httpserver.HttpExchange
import core.{BlockChain, StatementsCache}
import messages.{NewBlockMessage, SignedStatementMessage}
import peers.PeerAccess

trait MsgHandlerOps {

  val bcHttpServer: BCHttpServer
  val peerAccess: PeerAccess
  val bc: BlockChain
  val statementsCache: StatementsCache

  def processStatementAsFact(signedStatement: SignedStatementMessage, exchange: HttpExchange): Unit = {
    if (bc.containsFactInside(signedStatement.statement)) {
      bcHttpServer.sendHttpResponse(exchange, "Refused new block creation - existing fact.")
    } else {
      bc.addFactToNewBlock(signedStatement)
      statementsCache.remove(signedStatement.statement)
      peerAccess.sendMsg(NewBlockMessage(bc.getLatestBlock, bc.size - 1))
      bcHttpServer.sendHttpResponse(exchange, "New fact has been created and added to blockchain.")
    }
  }
}
