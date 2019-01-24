package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}

class GetChainHandler(hc: HttpContext) extends HttpHandler {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    hc.bcHttpServer.sendHttpResponse(exchange, hc.blockChain.serialize)
  }
}

