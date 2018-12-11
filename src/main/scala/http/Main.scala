package http

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import core.BlockChain

object Main extends App {
  val server = HttpServer.create()
  val bc = new BlockChain

  import java.net.InetSocketAddress

  server.bind(new InetSocketAddress(8765), 0)
  val context = server.createContext("/dumpchain", new GetChainHandler)
  server.start()

  import java.io.IOException

  class GetChainHandler extends HttpHandler {
    @throws[IOException]
    def handle(exchange: HttpExchange): Unit = {
      val sb = new StringBuffer
      val it = bc.chain.iterator()
      while (it.hasNext) {
        sb.append(it.next)
        if (it.hasNext)
          sb.append(",")
      }
      val bytes = sb.toString.getBytes
      exchange.sendResponseHeaders(200, bytes.length)
      val os = exchange.getResponseBody
      os.write(bytes)
      os.close
    }
  }

}
