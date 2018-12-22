package http

import java.net.InetSocketAddress
import java.security.KeyPair

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import core.{BlockChain, InitPayments}
import keys.ProdKeysFileOps
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory
import peers.PeerAccess

class BCHttpServer(port: Int, bc: BlockChain, peerAccess: PeerAccess, initPayments: InitPayments) {

  val logger = LoggerFactory.getLogger(this.getClass)

  def start(nodeName: String) = {
    val server = HttpServer.create()
    server.bind(new InetSocketAddress(port), 0)
    server.createContext("/dumpchain", new GetChainHandler(this, bc))
    server.createContext("/genkeys", new GenKeysHandler(ProdKeysFileOps, this))
    server.createContext("/nodeinfo", new NodeInfoHandler(nodeName, this, peerAccess))
    server.createContext("/addseeds", new AddSeedsHandler(this, peerAccess))
    server.createContext("/initpayment", new InitPaymentHandler(nodeName, this, initPayments, ProdKeysFileOps, peerAccess))
    server.createContext("/msgHandler", new MsgHandler(nodeName, this, initPayments, bc, ProdKeysFileOps, peerAccess))
    server.start()
  }

  def sendHttpResponse(exchange: HttpExchange, response: String): Unit = {
    sendHttpResponse(exchange, HttpStatus.SC_OK, response)
  }

  def sendHttpResponse(exchange: HttpExchange, code: Int, response: String): Unit = {
    if (code >= 400) {
      logger.error(response)
    } else {
      logger.info(response)
    }

    val bytes = response.getBytes
    exchange.sendResponseHeaders(code, bytes.length)
    val os = exchange.getResponseBody
    os.write(bytes)
    os.close
  }

}
