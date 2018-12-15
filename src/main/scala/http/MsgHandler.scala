package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.InitPayments
import http.BCHttpServer

import scala.io.Source

class MsgHandler(bcHttpServer: BCHttpServer, initPayments: InitPayments) extends HttpHandler {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    val msg = Source.fromInputStream(exchange.getRequestBody).getLines().mkString("\n")
    println(s"Received msg: $msg")
    bcHttpServer.sendHttpResponse(exchange, "")
  }
}