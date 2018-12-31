package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import config.Config
import messages.{AddPeersMessage, RequestAllStatementsMessage}
import org.apache.http.HttpStatus.SC_CREATED
import peers.PeerAccess
import util.{FileOps, HttpUtil}
import io.circe.generic.auto._
import scala.collection.JavaConverters._

import scala.io.Source

class AddSeedsHandler(bcHttpServer: BCHttpServer, peerAccess: PeerAccess, nodeName: String, fileOps: FileOps) extends HttpHandler with HttpUtil {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("PUT", exchange, bcHttpServer) {
      val source = Source.fromInputStream(exchange.getRequestBody)
      val seeds = source.getLines.mkString(",").split(",").map(_.trim)
      peerAccess.addAll(seeds)
      Config(peerAccess.peers.asScala.toSeq).write(nodeName, fileOps)
      peerAccess.sendMsg(AddPeersMessage(seeds, bcHttpServer.localServerAddress))
      peerAccess.sendMsg(RequestAllStatementsMessage(bcHttpServer.localServerAddress))
      source.close()
      bcHttpServer.sendHttpResponse(exchange, SC_CREATED, "New seeds have been added.")
    }
  }

}
