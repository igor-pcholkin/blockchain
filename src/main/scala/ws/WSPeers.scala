package ws

import java.util.concurrent.{ConcurrentLinkedQueue}

class WSPeers {
  val peers = new ConcurrentLinkedQueue[String]()

  def add(peer: String) = {
    if (!peers.contains(peer)) {
      peers.add(peer)
    }
  }

  def addAll(peers: Seq[String]) = {
    peers foreach (add(_))
  }
}
