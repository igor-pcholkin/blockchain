package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.StatementsCache
import util.DateTimeUtil

class GetStatementsHandler(bcHttpServer: BCHttpServer, statementsCache: StatementsCache) extends HttpHandler with DateTimeUtil {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {

    val statements = statementsCache.allStatementMessages.toSeq.sortBy(_.statement.timestamp) mkString "\n"

    bcHttpServer.sendHttpResponse(exchange, statements)
  }
}

