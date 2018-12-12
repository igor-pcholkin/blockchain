package http

import java.time.LocalDateTime

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import core.{Block, BlockChain}
import user.User

import scala.io.Source

object Main extends App {
  val server = HttpServer.create()
  val bc = new BlockChain
  val newBlock = Block(1, bc.getLatestBlock.hash, LocalDateTime.of(2018, 12, 24, 15, 0, 0), "Hi".getBytes)
  bc.add(newBlock)

  import java.net.InetSocketAddress

  server.bind(new InetSocketAddress(8765), 0)
  server.createContext("/dumpchain", new GetChainHandler)
  server.createContext("/postid", new PostIdHandler)
  server.start()

  import java.io.IOException

  class GetChainHandler extends HttpHandler {
    @throws[IOException]
    def handle(exchange: HttpExchange): Unit = {
      sendBytesToHttpResponse(exchange, bc.serialize)
    }
  }

  class PostIdHandler extends HttpHandler {
    @throws[IOException]
    def handle(exchange: HttpExchange): Unit = {
      val response = parseUserFromHttpRequest(exchange) match {
        case Some(user) =>
          s"User: ${user.id}, key: ${user.keyPair}"
        case None =>
          "Invalid user, expected: <user id>:<keyPair>"
      }
      sendBytesToHttpResponse(exchange, response.getBytes)
    }

    def parseUserFromHttpRequest(exchange: HttpExchange) = {
      if (exchange.getRequestMethod == "POST") {
        val s = Source.fromInputStream(exchange.getRequestBody).getLines.mkString
        val parts = s.split(":")
        val userId = parts(0)
        val key = parts(1)
        Some(User(userId, key))
      } else
        None
    }
  }

  def sendBytesToHttpResponse(exchange: HttpExchange, bytes: Array[Byte]) = {
    exchange.sendResponseHeaders(200, bytes.length)
    val os = exchange.getResponseBody
    os.write(bytes)
    os.close
  }
}
