package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.{BlockChain, StatementsCache}
import keys.KeysGenerator
import util.StringConverter
import peers.PeerAccess

class NodeInfoHandler(nodeName: String, bcHttpServer: BCHttpServer, peerAccess: PeerAccess, statementsCache: StatementsCache, bc: BlockChain)
  extends HttpHandler with KeysGenerator with StringConverter {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    val response = s"""Node name: $nodeName
                      |Peers: ${peerAccess.peers}
                      |Blocks in blockchain: ${bc.size}
                      |Size of statements cache: ${statementsCache.size}""".stripMargin
    bcHttpServer.sendHttpResponse(exchange, response)
  }
}

