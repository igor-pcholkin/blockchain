package http

import java.net.InetSocketAddress
import java.security.KeyPair

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import core.{BlockChain, InitPayments}
import keys.ProdKeysFileOps
import peers.PeerAccess

class BCHttpServer(port: Int, bc: BlockChain, peerAccess: PeerAccess, initPayments: InitPayments) {

  var mayBeKeyPair: Option[KeyPair] = None

  def setKeys(keyPair: KeyPair) = mayBeKeyPair = Some(keyPair)
  def getKeys = mayBeKeyPair
  def nonEmptyKeys = mayBeKeyPair.nonEmpty

  def start(nodeName: String) = {
    val server = HttpServer.create()
    server.bind(new InetSocketAddress(port), 0)
    server.createContext("/dumpchain", new GetChainHandler(this, bc))
    server.createContext("/genkeys", new GenKeysHandler(nodeName, ProdKeysFileOps, this))
    server.createContext("/nodeinfo", new NodeInfoHandler(nodeName, this, peerAccess))
    server.createContext("/addwspeers", new AddSeedsHandler(this, peerAccess))
    server.createContext("/initpayment", new InitPaymentHandler(nodeName, this, initPayments, ProdKeysFileOps, peerAccess))
    server.createContext("/msgHandler", new MsgHandler(this, initPayments, ProdKeysFileOps))
    server.start()
  }

  def sendHttpResponse(exchange: HttpExchange, response: String): Unit = {
    sendHttpResponse(exchange, 200, response)
  }

  def sendHttpResponse(exchange: HttpExchange, code: Int, response: String): Unit = {
    val bytes = response.getBytes
    exchange.sendResponseHeaders(code, bytes.length)
    val os = exchange.getResponseBody
    os.write(bytes)
    os.close
  }

}
