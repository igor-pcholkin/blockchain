package peers

import java.util.concurrent.ConcurrentLinkedQueue

import core.messages.Message
import io.circe.Encoder

import scala.concurrent.Future
import scala.collection.JavaConverters._

case class Result(status: Int, replyMsg: String)

object PeerAccess {
  def apply() = {
    new PeerAccess(new HttpPeerTransport)
  }
}

class PeerAccess(val peerTransport: PeerTransport) {
  val peers = new ConcurrentLinkedQueue[String]()

  def add(peer: String) = {
    if (!peers.contains(peer)) {
      peers.add(peer)
    }
  }

  def addAll(peers: Seq[String]) = {
    peers foreach (add(_))
  }

  def sendMsg[T <: Message](msg: T)(implicit encoder: Encoder[T]): Future[Result] = {
    peerTransport.sendMsg(msg, peers.asScala.toSeq)
  }
}
