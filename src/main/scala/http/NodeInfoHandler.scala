package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import keys.{KeysGenerator, KeysSerializator}
import util.Convert

class NodeInfoHandler(nodeName: String, bcHttpServer: BCHttpServer) extends HttpHandler with KeysGenerator with KeysSerializator with Convert {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    val publicKey = bcHttpServer.getKeys match {
      case Some(keyPair) => keyPair.getPublic
      case None => "(None)"
    }
    val response = s"""Node name: $nodeName
                      |Public key: ${publicKey}""".stripMargin
    bcHttpServer.sendBytesToHttpResponse(exchange, response.getBytes)
  }
}

