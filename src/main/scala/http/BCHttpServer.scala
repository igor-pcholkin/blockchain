package http

import java.net.InetSocketAddress

import com.sun.net.httpserver.{HttpExchange, HttpServer}
import core.{BlockChain, StatementsCache}
import keys.ProdKeysFileOps
import messages.PullNewsMessage
import org.apache.http.HttpStatus
import org.slf4j.{Logger, LoggerFactory}
import peers.PeerAccess
import util.ProdFileOps

class BCHttpServer(val localHost: LocalHost, bc: BlockChain, peerAccess: PeerAccess, statementsCache: StatementsCache) {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val server: HttpServer = HttpServer.create()

  def start(nodeName: String): Unit = {
    server.bind(new InetSocketAddress(localHost.localPort), 0)
    val httpContext = HttpContext(nodeName, this, bc, statementsCache, peerAccess, ProdKeysFileOps, ProdFileOps)
    server.createContext("/dumpchain", new GetChainHandler(httpContext))
    server.createContext("/genkeys", new GenKeysHandler(httpContext))
    server.createContext("/nodeinfo", new NodeInfoHandler(httpContext))
    server.createContext("/addseeds", new AddSeedsHandler(httpContext))
    server.createContext("/initpayment", new InitPaymentHandler(httpContext))
    server.createContext("/msgHandler", new MsgHandler(httpContext))
    server.createContext("/getfacts", new GetFactsHandler(httpContext))
    server.createContext("/getstatements", new GetStatementsHandler(httpContext))
    server.createContext("/registerUser", new RegisterUserHandler(httpContext))
    server.createContext("/approveFact", new ApproveFactHandler(httpContext))
    server.createContext("/users", new UsersHandler(httpContext))
    server.start()

    scheduleSendingPullNewsMessage()

  }

  private def scheduleSendingPullNewsMessage() = {
    val t = new java.util.Timer()
    val task = new java.util.TimerTask {
      def run() = peerAccess.sendMsg(PullNewsMessage(bc.size))
    }
    t.schedule(task, 0L, 60000L)
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
