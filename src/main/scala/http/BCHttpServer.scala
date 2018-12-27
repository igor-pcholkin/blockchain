package http

import java.net.{InetSocketAddress, Socket}

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import core.{BlockChain, Statements}
import keys.ProdKeysFileOps
import org.apache.http.HttpStatus
import org.slf4j.{Logger, LoggerFactory}
import peers.PeerAccess

class BCHttpServer(port: Int, bc: BlockChain, peerAccess: PeerAccess, statements: Statements) {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val server: HttpServer = HttpServer.create()

  def start(nodeName: String): Unit = {
    server.bind(new InetSocketAddress(port), 0)
    server.createContext("/dumpchain", new GetChainHandler(this, bc))
    server.createContext("/genkeys", new GenKeysHandler(nodeName, ProdKeysFileOps, this))
    server.createContext("/nodeinfo", new NodeInfoHandler(nodeName, this, peerAccess))
    server.createContext("/addseeds", new AddSeedsHandler(this, peerAccess))
    server.createContext("/initpayment", new InitPaymentHandler(nodeName, this, statements, ProdKeysFileOps, peerAccess))
    server.createContext("/msgHandler", new MsgHandler(nodeName, this, statements, bc, ProdKeysFileOps, peerAccess))
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
    os.close()
  }

  lazy val localServerAddress: String = {
    // ugly, but it works... a better way?
    val socket = new Socket()
    socket.connect(new InetSocketAddress("google.com", 80))
    socket.getLocalAddress.toString.split("/")(1) + ":" + port
  }
}
