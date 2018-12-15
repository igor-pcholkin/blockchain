package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import peers.PeerAccess

import scala.io.Source

class AddSeedsHandler(bcHttpServer: BCHttpServer, peerAccess: PeerAccess) extends HttpHandler {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    if (exchange.getRequestMethod == "PUT") {
      val s = Source.fromInputStream(exchange.getRequestBody)
      val seeds = s.getLines.mkString(",").split(",").map(_.trim)
      peerAccess.addAll(seeds)
      s.close()
      bcHttpServer.sendHttpResponse(exchange, 201, "New seeds have been added.")
    } else {
      bcHttpServer.sendHttpResponse(exchange, 400, "Invalid method, use PUT")
    }
  }
}
