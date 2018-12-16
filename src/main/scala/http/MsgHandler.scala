package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.{InitPaymentMessage, InitPayments}

import scala.io.Source

class MsgHandler(bcHttpServer: BCHttpServer, initPayments: InitPayments) extends HttpHandler {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    val msgAsString = Source.fromInputStream(exchange.getRequestBody).getLines().mkString("\n")
    val message = InitPaymentMessage.deserialize(msgAsString) match {
      case Right(initPaymentMessage) =>
        println(s"Deser msg: $initPaymentMessage")
        initPaymentMessage.toString
      case Left(error) =>
        println(s"err: ${error.getMessage}")
        error.getMessage
    }
    bcHttpServer.sendHttpResponse(exchange, message)
  }
}