package peers

import java.util.concurrent.ConcurrentLinkedQueue

import core.Message
import io.circe.Encoder

import scala.concurrent.Future
import scala.collection.JavaConverters._
import scala.collection.mutable

case class Result(status: Int, replyMsg: String)

object PeerAccess {
  def apply(): PeerAccess = {
    new PeerAccess(new HttpPeerTransport)
  }
}

class PeerAccess(val peerTransport: PeerTransport) {
  val peers = new ConcurrentLinkedQueue[String]()
  val msgToPeers = new mutable.HashMap[Message, Seq[String]]

  def add(peer: String): Unit = {
    if (!peers.contains(peer)) {
      peers.add(peer)
    }
  }

  def addAll(peers: Seq[String]): Unit = {
    peers foreach add
  }

  def sendMsg[T <: Message](msg: T)(implicit encoder: Encoder[T]): Future[Result] = {
    val peersReceivedMsg = msgToPeers.getOrElse(msg, Nil)
    val peersToSendMessage = peers.asScala.toSeq.diff(peersReceivedMsg)
    msgToPeers.put(msg, peersReceivedMsg ++ peersToSendMessage)
    peerTransport.sendMsg(msg, peersToSendMessage)
  }
}
