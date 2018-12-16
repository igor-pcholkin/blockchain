package peers

import core.Message
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClients

import scala.concurrent.Future
import util.FutureTimeout._

import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global

abstract class PeerTransport {
  def sendMsg(msg: Message, peers: Seq[String]): Future[Result]
}

class HttpPeerTransport extends PeerTransport {
  override def sendMsg(msg: Message, peers: Seq[String]) = {
    val f = Future.sequence(
      peers map { peer =>
        Future {
          postRequest(msg.serialize, peer)
        }
      }).withTimeout
    Future.successful(Result(200, "Msg sent to all peers."))
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
