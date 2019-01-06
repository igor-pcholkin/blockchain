package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.BlockChain
import json.FactJson

import scala.collection.JavaConverters._

class GetFactsHandler(bcHttpServer: BCHttpServer, bc: BlockChain) extends HttpHandler {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    val facts = bc.chain.iterator().asScala flatMap { block =>
      FactJson.deserialize(new String(block.data)) match {
        case Right(fact) => Some(fact.statement.toString)
        case Left(_) => None
      }
    } mkString "\n"

    bcHttpServer.sendHttpResponse(exchange, facts)
  }
}

