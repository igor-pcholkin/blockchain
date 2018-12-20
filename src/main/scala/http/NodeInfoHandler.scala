package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import keys.{KeysGenerator}
import util.StringConverter
import peers.PeerAccess

class NodeInfoHandler(nodeName: String, bcHttpServer: BCHttpServer, peerAccess: PeerAccess) extends HttpHandler with KeysGenerator with StringConverter {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    val publicKey = bcHttpServer.getKeys match {
      case Some(keyPair) => keyPair.getPublic
      case None => "(None)"
    }
    val response = s"""Node name: $nodeName
                      |Public key: ${publicKey}
                      |WebSocket peers: ${peerAccess.peers}""".stripMargin
    bcHttpServer.sendHttpResponse(exchange, response)
  }
}

