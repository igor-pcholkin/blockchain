package util

import com.sun.net.httpserver.HttpExchange
import http.BCHttpServer
import org.apache.http.HttpStatus.SC_BAD_REQUEST

trait HttpUtil {
  def withHttpMethod(httpMethod: String, exchange: HttpExchange, bcHttpServer: BCHttpServer)(code : => Unit): Unit = {
    if (exchange.getRequestMethod == httpMethod) {
      code
    } else {
      bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, s"Invalid method ${exchange.getRequestMethod}, use $httpMethod")
    }
  }

  def getRequestParam(mayBeQuery: Option[String], paramName: String): Option[String] = {
    mayBeQuery.flatMap { query =>
      query.split("&").flatMap { param =>
        val paramPair = param.split("=")
        if (paramPair(0) == paramName)
          Some(paramPair(1))
        else
          None
      }.find(_.nonEmpty)
    }
  }
}
