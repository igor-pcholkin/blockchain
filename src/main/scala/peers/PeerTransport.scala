package peers

import core.messages.Message
import io.circe.Encoder
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClients

import scala.concurrent.Future
import util.FutureTimeout._

import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global

abstract class PeerTransport {
  def sendMsg[T <: Message](msg: T, peers: Seq[String])(implicit encoder: Encoder[T]): Future[Result]
}

class HttpPeerTransport extends PeerTransport {
  override def sendMsg[T <: Message](msg: T, peers: Seq[String])(implicit encoder: Encoder[T]): Future[Result] = {
    val f = Future.sequence(
      peers map { peer =>
        Future {
          postRequest(Message.serialize(msg), peer)
        }
      }).withTimeout
    Future.successful(Result(HttpStatus.SC_OK, "Msg sent to all peers."))
  }

  private def postRequest(msg: String, peer: String) = {
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
