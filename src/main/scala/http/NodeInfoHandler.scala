package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import keys.KeysGenerator
import util.StringConverter
import peers.PeerAccess

class NodeInfoHandler(nodeName: String, bcHttpServer: BCHttpServer, peerAccess: PeerAccess) extends HttpHandler with KeysGenerator with StringConverter {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    val response = s"""Node name: $nodeName
                      |Peers: ${peerAccess.peers}""".stripMargin
    bcHttpServer.sendHttpResponse(exchange, response)
  }
}

