package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import config.Config
import messages.{AddPeersMessage, PullNewsMessage}
import org.apache.http.HttpStatus.SC_CREATED
import util.HttpUtil

import scala.collection.JavaConverters._
import scala.io.Source

class AddSeedsHandler(hc: HttpContext) extends HttpHandler with HttpUtil {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("PUT", exchange, hc.bcHttpServer) {
      val source = Source.fromInputStream(exchange.getRequestBody)
      val seeds = source.getLines.mkString(",").split(",").map(_.trim)
      hc.peerAccess.addAll(seeds)
      Config(hc.peerAccess.peers.asScala.toSeq).write(hc.nodeName, hc.fileOps)
      hc.peerAccess.sendMsg(AddPeersMessage(seeds))
      hc.peerAccess.sendMsg(PullNewsMessage(hc.blockChain.size))
      source.close()
      hc.bcHttpServer.sendHttpResponse(exchange, SC_CREATED, "New seeds have been added.")
    }
  }

}
