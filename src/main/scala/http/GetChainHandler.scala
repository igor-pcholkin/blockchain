package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.BlockChain

class GetChainHandler(bcHttpServer: BCHttpServer, bc: BlockChain) extends HttpHandler {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    bcHttpServer.sendHttpResponse(exchange, bc.serialize)
  }
}

