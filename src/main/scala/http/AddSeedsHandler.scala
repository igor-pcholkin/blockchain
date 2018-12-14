package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import ws.WSPeers

import scala.io.Source

class AddSeedsHandler(bcHttpServer: BCHttpServer, wsPeers: WSPeers) extends HttpHandler {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    if (exchange.getRequestMethod == "PUT") {
      val s = Source.fromInputStream(exchange.getRequestBody)
      val peers = s.getLines.mkString(",").split(",").map(_.trim)
      wsPeers.addAll(peers)
      s.close()
      bcHttpServer.sendHttpResponse(exchange, 201, "New WS seeds have been added.")
    } else {
      bcHttpServer.sendHttpResponse(exchange, 400, "Invalid method, use PUT")
    }
  }
}
