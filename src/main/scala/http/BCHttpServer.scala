package http

import java.net.InetSocketAddress

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import core.{BlockChain, StatementsCache}
import keys.ProdKeysFileOps
import messages.{RequestAllStatementsMessage, RequestBlocksMessage}
import org.apache.http.HttpStatus
import org.slf4j.{Logger, LoggerFactory}
import peers.PeerAccess
import util.ProdFileOps
import io.circe.generic.auto._

class BCHttpServer(val localHost: LocalHost, bc: BlockChain, peerAccess: PeerAccess, statementsCache: StatementsCache) {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val server: HttpServer = HttpServer.create()

  def start(nodeName: String): Unit = {
    server.bind(new InetSocketAddress(localHost.localPort), 0)
    server.createContext("/dumpchain", new GetChainHandler(this, bc))
    server.createContext("/genkeys", new GenKeysHandler(nodeName, ProdKeysFileOps, this))
    server.createContext("/nodeinfo", new NodeInfoHandler(nodeName, this, peerAccess))
    server.createContext("/addseeds", new AddSeedsHandler(this, peerAccess, nodeName, ProdFileOps, bc))
    server.createContext("/initpayment", new InitPaymentHandler(nodeName, this, statementsCache, ProdKeysFileOps, peerAccess, bc))
    server.createContext("/msgHandler", new MsgHandler(nodeName, this, statementsCache, bc, ProdKeysFileOps, peerAccess))
    server.createContext("/getfacts", new GetFactsHandler(this, bc))
    server.start()

    peerAccess.sendMsg(RequestAllStatementsMessage())
    peerAccess.sendMsg(RequestBlocksMessage(bc.chain.size()))
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

}
