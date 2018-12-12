package http

import java.time.LocalDateTime

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import core.{Block, BlockChain}

object Main extends App {
  val server = HttpServer.create()
  val bc = new BlockChain
  val newBlock = Block(1, bc.getLatestBlock.hash, LocalDateTime.of(2018, 12, 24, 15, 0, 0), "Hi".getBytes)
  bc.add(newBlock)

  import java.net.InetSocketAddress

  server.bind(new InetSocketAddress(8765), 0)
  val context = server.createContext("/dumpchain", new GetChainHandler)
  server.start()

  import java.io.IOException

  class GetChainHandler extends HttpHandler {
    @throws[IOException]
    def handle(exchange: HttpExchange): Unit = {
      val bytes = bc.serialize
      exchange.sendResponseHeaders(200, bytes.length)
      val os = exchange.getResponseBody
      os.write(bytes)
      os.close
    }
  }

}
