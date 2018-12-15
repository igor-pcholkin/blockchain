package http

import core.{BlockChain, InitPayments}
import peers.{HttpPeerTransport, PeerAccess}

object Main extends App {
  val (nodeName, port) = if (args.length < 2) {
    System.err.println("Node name and port should be provided")
    System.exit(1)
    ("", 2)
  } else {
    (args(0), args(1).toInt)
  }

  val bc = new BlockChain
  val peerAccess = new PeerAccess(new HttpPeerTransport)
  val initPayments = new InitPayments

  new BCHttpServer(port, bc, peerAccess, initPayments).start(nodeName)
}
