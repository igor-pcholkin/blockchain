package peers

import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.HttpClients

import scala.concurrent.Future
import util.TimeoutException

import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global

abstract class PeerTransport {
  def sendMsg(msg: String, peer: String): Future[Result]
}

class HttpPeerTransport extends PeerTransport {
  override def sendMsg(msg: String, peer: String): Future[Result] = {
    Future.firstCompletedOf(List(
      Future {
        postRequest(msg, peer)
      },
      Future {
        Thread.sleep(500)
        throw new TimeoutException
      }
    ))
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
