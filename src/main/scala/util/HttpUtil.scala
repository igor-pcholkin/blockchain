package util

import com.sun.net.httpserver.HttpExchange
import http.BCHttpServer
import org.apache.http.HttpStatus.SC_BAD_REQUEST

object HttpUtil {
  def withHttpMethod(httpMethod: String, exchange: HttpExchange, bcHttpServer: BCHttpServer)(code : => Unit) = {
    if (exchange.getRequestMethod == httpMethod) {
      code
    } else {
      bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, s"Invalid method, use $httpMethod")
    }
  }
}
