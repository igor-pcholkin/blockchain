package http

import core.{BlockChain}
import ws.WSPeers

object Main extends App {
  val nodeName: String = if (args.length == 0) {
    System.err.println("Node name should be provided")
    System.exit(1)
    ""
  } else {
    args(0)
  }

  val bc = new BlockChain
  val wsPeers = new WSPeers

  new BCHttpServer(bc, wsPeers).start(nodeName)
}
