package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import org.slf4j.{Logger, LoggerFactory}
import util.StringConverter

class GetFactsHandler(hc: HttpContext) extends HttpHandler with StringConverter {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    val facts = hc.blockChain.blocksFrom(0) flatMap { block =>
      hc.blockChain.extractFact(block) match {
        case Right(fact) => Some(fact.statement.toString + ":" + block.factHash)
        case Left(_) =>
          logger.error(s"Cannot extract fact: ${new String(block.data)}")
          None
      }
    } mkString "\n"

    hc.bcHttpServer.sendHttpResponse(exchange, facts)
  }
}

