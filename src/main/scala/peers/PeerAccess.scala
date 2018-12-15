package peers

import java.util.concurrent.ConcurrentLinkedQueue

import core.Message
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClients
import util.FutureTimeout._

import scala.concurrent.Future
import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global

case class Result(status: Int, replyMsg: String)

abstract class PeerTransport {
  def sendMsg(msg: Message, peer: String): Future[Result]
}

class HttpPeerTransport extends PeerTransport {
  override def sendMsg(msg: Message, peer: String) = {
    Future {
      postRequest(msg.toString, peer)
    } withTimeout
  }

  def postRequest(msg: String, peer: String) = {
    val url = s"http://$peer/msgHandler"

    val post = new HttpPost(url)
    post.setEntity(new StringEntity(msg, ContentType.TEXT_PLAIN))
    val client = HttpClients.createDefault()

    // send the post request
    val response = client.execute(post)
    val responseData = Source.fromInputStream(response.getEntity.getContent).getLines.mkString("\n")
    Result(response.getStatusLine.getStatusCode, responseData)
  }

}

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

  def sendMsg(msg: Message): Future[Result] = {
    peers.forEach { peer =>
      peerTransport.sendMsg(msg, peer)
    }
    Future.successful(Result(200, "Msg sent to all peers."))
  }
}
