package http

import core.{BlockChain, InitPayments}
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
  val initPayments = new InitPayments

  new BCHttpServer(bc, wsPeers, initPayments).start(nodeName)
}
