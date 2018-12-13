package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import keys.{KeysGenerator, KeysSerializator}
import util.Convert
import ws.WSPeers

class NodeInfoHandler(nodeName: String, bcHttpServer: BCHttpServer, wsPeers: WSPeers) extends HttpHandler with KeysGenerator with KeysSerializator with Convert {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    val publicKey = bcHttpServer.getKeys match {
      case Some(keyPair) => keyPair.getPublic
      case None => "(None)"
    }
    val response = s"""Node name: $nodeName
                      |Public key: ${publicKey}
                      |WebSocket peers: ${wsPeers.peers}""".stripMargin
    bcHttpServer.sendHttpResponse(exchange, response)
  }
}

