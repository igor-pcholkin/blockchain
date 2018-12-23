package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import keys.{KeysFileOps, KeysGenerator, KeysSerializator}
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_CREATED}
import util.{HttpUtil, StringConverter}

class GenKeysHandler(nodeName: String, val keysFileOps: KeysFileOps, bcHttpServer: BCHttpServer) extends HttpHandler with KeysGenerator
  with KeysSerializator with StringConverter with HttpUtil {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("PUT", exchange, bcHttpServer) {
      val mayBeQuery = Option(exchange.getRequestURI.getQuery)
      getRequestParam(mayBeQuery, "userName") match {
        case Some(userName) =>
          val overwrite = getRequestParam(mayBeQuery, "overwrite").getOrElse("false").toBoolean
          val isUserKeysDirExists = keysFileOps.isKeysDirExists(nodeName, userName)
          if (isUserKeysDirExists && !overwrite) {
            bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "Public or private key already exists, use overwrite=true to overwrite")
          } else {
            val keyPair = generateKeyPair()
            writeKey(nodeName, userName, keyPair.getPrivate)
            writeKey(nodeName, userName, keyPair.getPublic)
            bcHttpServer.sendHttpResponse(exchange, SC_CREATED, "New keys have been created")
          }
        case None =>
          bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "User name should be specified in request query (userName)")
      }
    }
  }
}

