package http

import core.{Block, BlockChain}

object Main extends App {
  val nodeName: String = if (args.length == 0) {
    System.err.println("Node name should be provided")
    System.exit(1)
    ""
  } else {
    args(0)
  }

  val bc = new BlockChain

  new BCHttpServer().start(nodeName)
}
