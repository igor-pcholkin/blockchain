package http

import java.io.{IOException}

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import keys.{KeysFileOps, KeysGenerator, KeysSerializator}
import util.StringConverter

class GenKeysHandler(nodeName: String, val keysFileOps: KeysFileOps, bcHttpServer: BCHttpServer) extends HttpHandler with KeysGenerator with KeysSerializator with StringConverter{
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    if (exchange.getRequestMethod == "PUT") {
      if (bcHttpServer.nonEmptyKeys && exchange.getRequestURI().getQuery != "overwrite=true") {
        bcHttpServer.sendHttpResponse(exchange, 400, "Public or private key already exists, use overwrite=true to overwrite")
      } else {
        val keyPair = generateKeyPair()
        if (!keysFileOps.isKeysDirExists(nodeName)) {
          keysFileOps.createKeysDir(nodeName)
        }
        writeKey(nodeName, keyPair.getPrivate)
        writeKey(nodeName, keyPair.getPublic)
        bcHttpServer.setKeys(keyPair)
        bcHttpServer.sendHttpResponse(exchange, 201, "New keys have been created")
      }
    } else {
      bcHttpServer.sendHttpResponse(exchange, 400, "Invalid method, use PUT")
    }
  }
}

