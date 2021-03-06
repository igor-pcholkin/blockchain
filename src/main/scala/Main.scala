import config.Config
import core.{ProdBlockChain, StatementsCache}
import http.{BCHttpServer, LocalHost}
import org.slf4j.LoggerFactory
import peers.{HttpPeerTransport, PeerAccess}
import util.ProdFileOps

object Main extends App {

  val logger = LoggerFactory.getLogger(this.getClass)

  val (nodeName, port) = if (args.length < 2) {
    logger.error("Node name and port should be provided")
    System.exit(1)
    ("", 2)
  } else {
    (args(0), args(1).toInt)
  }

  logger.info(s"Started as $nodeName, listening port $port")

  val config = Config.read(nodeName, ProdFileOps)
  val bc = new ProdBlockChain(nodeName)
  val localHost = LocalHost(port)
  val peerAccess = new PeerAccess(new HttpPeerTransport, localHost, config.seeds)
  val statementsCache = new StatementsCache

  new BCHttpServer(localHost, bc, peerAccess, statementsCache).start(nodeName)
}
