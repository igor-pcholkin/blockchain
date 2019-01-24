package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import util.DateTimeUtil

class GetStatementsHandler(hc: HttpContext) extends HttpHandler with DateTimeUtil {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {

    val statements = hc.statementsCache.allStatementMessages.toSeq.sortBy(_.statement.timestamp) mkString "\n"

    hc.bcHttpServer.sendHttpResponse(exchange, statements)
  }
}

