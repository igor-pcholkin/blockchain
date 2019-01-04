package peers

import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}

import core.Message
import http.LocalHost
import io.circe.Encoder
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

case class Result(status: Int, replyMsg: String)

object PeerAccess {
  def apply(localHost: LocalHost): PeerAccess = {
    new PeerAccess(new HttpPeerTransport, localHost)
  }
}

class PeerAccess(val peerTransport: PeerTransport, val localHost: LocalHost, seeds: Seq[String] = Nil) {
  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val peers = new ConcurrentLinkedQueue[String]()

  val msgToPeers = new ConcurrentHashMap[Message, Seq[String]]

  addAll(seeds)

  def add(peer: String): Unit = {
    if (!peers.contains(peer) && peer != localHost.localServerAddress && peer != s"localhost:${localHost.localPort}") {
      peers.add(peer)
    }
  }

  def addAll(peers: Seq[String]): Unit = {
    peers foreach add
  }

  def sendMsg[T <: Message](msg: T)(implicit encoder: Encoder[T]): Future[Seq[String]] = {
    logger.debug(s"Sending message: $msg to all peers")
    logger.debug(s"Message to peers ${msgToPeers.toString}")

    val peersReceivedMsgBefore = msgToPeers.asScala.getOrElse(msg, Nil)
    val peersToSendMessage = peers.asScala.toSeq.diff(peersReceivedMsgBefore)

    Future.sequence(peersToSendMessage map { peer =>
      logger.debug(s"Message sent to peer $peer")
      peerTransport.sendMsg(msg, peer).map (_ => Some(peer)).recover { case ex => None }
    }) map (_.flatten) map { peersReceivedMsgNow =>
      logger.debug(s"Adding to (Message to peers) peers $peersReceivedMsgNow for msg $msg")
      msgToPeers.put(msg, peersReceivedMsgBefore ++ peersReceivedMsgNow)
    }
  }

  def sendMsg[T <: Message](msg: T, peer: String)(implicit encoder: Encoder[T]): Unit = {
    logger.debug(s"Sending message: $msg to peer $peer")
    logger.debug(s"Message to peers ${msgToPeers.toString}")

    val peersReceivedMsgBefore = msgToPeers.asScala.getOrElse(msg, Nil)
    if (!peersReceivedMsgBefore.contains(peer)) {
      logger.debug(s"Message sent to peer $peer")
      peerTransport.sendMsg(msg, peer) map { _ =>
        msgToPeers.put(msg, peersReceivedMsgBefore :+ peer)
      }
    }
  }

}
