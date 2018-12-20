package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_CREATED}
import peers.PeerAccess
import util.HttpUtil.withHttpMethod

import scala.io.Source

class AddSeedsHandler(bcHttpServer: BCHttpServer, peerAccess: PeerAccess) extends HttpHandler {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("PUT", exchange, bcHttpServer) {
      val s = Source.fromInputStream(exchange.getRequestBody)
      val seeds = s.getLines.mkString(",").split(",").map(_.trim)
      peerAccess.addAll(seeds)
      s.close()
      bcHttpServer.sendHttpResponse(exchange, SC_CREATED, "New seeds have been added.")
    }
  }
}
