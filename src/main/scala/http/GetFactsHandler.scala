package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.BlockChain
import org.slf4j.{Logger, LoggerFactory}
import util.StringConverter

class GetFactsHandler(bcHttpServer: BCHttpServer, bc: BlockChain) extends HttpHandler with StringConverter {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    val facts = bc.blocksFrom(0) flatMap { block =>
      bc.extractFact(block) match {
        case Right(fact) => Some(fact.statement.toString + ":" + bytesToBase64Str(block.hash))
        case Left(_) =>
          logger.error(s"Cannot extract fact: ${new String(block.data)}")
          None
      }
    } mkString "\n"

    bcHttpServer.sendHttpResponse(exchange, facts)
  }
}

