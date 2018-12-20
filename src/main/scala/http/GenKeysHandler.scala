package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import keys.{KeysFileOps, KeysGenerator, KeysSerializator}
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_CREATED}
import util.StringConverter
import util.HttpUtil.withHttpMethod

class GenKeysHandler(nodeName: String, val keysFileOps: KeysFileOps, bcHttpServer: BCHttpServer) extends HttpHandler with KeysGenerator with KeysSerializator with StringConverter{
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("PUT", exchange, bcHttpServer) {
      if (bcHttpServer.nonEmptyKeys && exchange.getRequestURI().getQuery != "overwrite=true") {
        bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "Public or private key already exists, use overwrite=true to overwrite")
      } else {
        val keyPair = generateKeyPair()
        if (!keysFileOps.isKeysDirExists(nodeName)) {
          keysFileOps.createKeysDir(nodeName)
        }
        writeKey(nodeName, keyPair.getPrivate)
        writeKey(nodeName, keyPair.getPublic)
        bcHttpServer.setKeys(keyPair)
        bcHttpServer.sendHttpResponse(exchange, SC_CREATED, "New keys have been created")
      }
    }
  }
}

