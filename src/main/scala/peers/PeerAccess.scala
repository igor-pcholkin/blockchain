package peers

import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}

import core.Message
import io.circe.Encoder
import org.apache.http.HttpStatus

import scala.concurrent.Future
import scala.collection.JavaConverters._

case class Result(status: Int, replyMsg: String)

object PeerAccess {
  def apply(): PeerAccess = {
    new PeerAccess(new HttpPeerTransport)
  }
}

class PeerAccess(val peerTransport: PeerTransport, seeds: Seq[String] = Nil) {
  val peers = new ConcurrentLinkedQueue[String]()

  val msgToPeers = new ConcurrentHashMap[Message, Seq[String]]

  addAll(seeds)

  def add(peer: String): Unit = {
    if (!peers.contains(peer)) {
      peers.add(peer)
    }
  }

  def addAll(peers: Seq[String]): Unit = {
    peers foreach add
  }

  def sendMsg[T <: Message](msg: T)(implicit encoder: Encoder[T]): Future[Result] = {
    val peersReceivedMsg = msgToPeers.asScala.getOrElse(msg, Nil)
    val peersToSendMessage = peers.asScala.toSeq.diff(peersReceivedMsg)
    msgToPeers.put(msg, peersReceivedMsg ++ peersToSendMessage)
    peerTransport.sendMsg(msg, peersToSendMessage)
  }

  def sendMsg[T <: Message](msg: T, peer: String)(implicit encoder: Encoder[T]): Future[Result] = {
    if (!msgToPeers.asScala.getOrElse(msg, Nil).contains(peer)) {
      peerTransport.sendMsg(msg, Seq(peer))
    } else {
      Future.successful(Result(HttpStatus.SC_OK, "Msg sent already."))
    }
  }
}
