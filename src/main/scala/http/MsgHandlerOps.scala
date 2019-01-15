package http

import com.sun.net.httpserver.HttpExchange
import core.{BlockChain, StatementsCache}
import messages.{NewBlockMessage, SignedStatementMessage}
import peers.PeerAccess

trait MsgHandlerOps {

  val bcHttpServer: BCHttpServer
  val peerAccess: PeerAccess
  val bc: BlockChain

  def processStatementAsFact(signedStatement: SignedStatementMessage, exchange: HttpExchange): Unit = {
    bc.addFactToNewBlock(signedStatement)
    peerAccess.sendMsg(NewBlockMessage(bc.getLatestBlock, bc.size - 1))
    bcHttpServer.sendHttpResponse(exchange, "New fact has been created and added to blockchain.")
  }
}
