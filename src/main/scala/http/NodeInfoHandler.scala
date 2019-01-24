package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import keys.KeysGenerator
import util.StringConverter

class NodeInfoHandler(hc: HttpContext)
  extends HttpHandler with KeysGenerator with StringConverter {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    val response = s"""Node name: ${hc.nodeName}
                      |Peers: ${hc.peerAccess.peers}
                      |Blocks in blockchain: ${hc.blockChain.size}
                      |Size of statements cache: ${hc.statementsCache.size}""".stripMargin
    hc.bcHttpServer.sendHttpResponse(exchange, response)
  }
}

