package http

import java.net.InetSocketAddress
import java.security.KeyPair

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import http.Main.bc
import keys.{KeysGenerator, KeysSerializator}
import util.Convert

class BCHttpServer {

  var mayBeKeyPair: Option[KeyPair] = None

  def setKeys(keyPair: KeyPair) = mayBeKeyPair = Some(keyPair)
  def getKeys = mayBeKeyPair
  def nonEmptyKeys = mayBeKeyPair.nonEmpty

  def start(nodeName: String) = {
    val server = HttpServer.create()
    server.bind(new InetSocketAddress(8765), 0)
    server.createContext("/dumpchain", new GetChainHandler)
    server.createContext("/genkeys", new GenKeysHandler(nodeName, new ProdKeysFileOps, this))
    server.createContext("/nodeinfo", new NodeInfoHandler(nodeName, this))
    server.start()
  }

  import java.io.IOException

  class GetChainHandler extends HttpHandler {
    @throws[IOException]
    def handle(exchange: HttpExchange): Unit = {
      sendBytesToHttpResponse(exchange, bc.serialize)
    }
  }

  def sendBytesToHttpResponse(exchange: HttpExchange, bytes: Array[Byte]) = {
    sendHttpResponse(exchange, 200, bytes)
  }

  def sendHttpResponse(exchange: HttpExchange, code: Int, bytes: Array[Byte]) = {
    exchange.sendResponseHeaders(code, bytes.length)
    val os = exchange.getResponseBody
    os.write(bytes)
    os.close
  }

}
