package peers

import http.LocalHost
import messages.AddPeersMessage
import io.circe.Encoder
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest
import org.scalatest.mockito.MockitoSugar
import org.scalatest.FlatSpec
import io.circe.generic.auto._
import org.apache.http.HttpStatus

import scala.collection.JavaConverters._
import scala.concurrent.Future

class PeerAccessTest extends FlatSpec with scalatest.Matchers with MockitoSugar {
  "PeerAccess" should "allow to send the same message to the same peer only once when broadcasting the message" in {
    val transport = mock[PeerTransport]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = new PeerAccess(transport, mockLocalHost)
    peerAccess.add("p1")
    val addPeersMessage1 = AddPeersMessage(Seq("1"), "localhost")
    val addPeersMessage2 = AddPeersMessage(Seq("2"), "localhost")

    when(transport.sendMsg(Matchers.eq(addPeersMessage1), Matchers.eq("p1"))(Matchers.any[Encoder[AddPeersMessage]])).thenReturn(Future.successful(Result(HttpStatus.SC_OK, "OK.")))
    when(transport.sendMsg(Matchers.eq(addPeersMessage2), Matchers.eq("p1"))(Matchers.any[Encoder[AddPeersMessage]])).thenReturn(Future.successful(Result(HttpStatus.SC_OK, "OK.")))

    peerAccess.sendMsg(addPeersMessage1)
    peerAccess.sendMsg(addPeersMessage1)
    peerAccess.sendMsg(addPeersMessage2)
    verify(transport, times(1)).sendMsg(Matchers.eq(addPeersMessage1), Matchers.eq("p1"))(Matchers.any[Encoder[AddPeersMessage]])
    verify(transport, times(1)).sendMsg(Matchers.eq(addPeersMessage2), Matchers.eq("p1"))(Matchers.any[Encoder[AddPeersMessage]])
  }

  it should "allow to send the same message to the same peer only once when sending the message directly to peer" in {
    val transport = mock[PeerTransport]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = new PeerAccess(transport, mockLocalHost)
    peerAccess.add("p1")
    val addPeersMessage1 = AddPeersMessage(Seq("1"), "localhost")
    val addPeersMessage2 = AddPeersMessage(Seq("2"), "localhost")

    when(transport.sendMsg(Matchers.eq(addPeersMessage1), Matchers.eq("p1"))(Matchers.any[Encoder[AddPeersMessage]])).thenReturn(Future.successful(Result(HttpStatus.SC_OK, "OK.")))
    when(transport.sendMsg(Matchers.eq(addPeersMessage2), Matchers.eq("p1"))(Matchers.any[Encoder[AddPeersMessage]])).thenReturn(Future.successful(Result(HttpStatus.SC_OK, "OK.")))

    peerAccess.sendMsg(addPeersMessage1, "p1")
    peerAccess.sendMsg(addPeersMessage1, "p1")
    peerAccess.sendMsg(addPeersMessage2, "p1")
    verify(transport, times(1)).sendMsg(Matchers.eq(addPeersMessage1), Matchers.eq("p1"))(Matchers.any[Encoder[AddPeersMessage]])
    verify(transport, times(1)).sendMsg(Matchers.eq(addPeersMessage2), Matchers.eq("p1"))(Matchers.any[Encoder[AddPeersMessage]])
  }

  it should "not allow to add existing peers and localhost" in {
    val transport = mock[PeerTransport]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = new PeerAccess(transport, mockLocalHost)

    when(mockLocalHost.localServerAddress).thenReturn("128.33.45.55:5761")
    when(mockLocalHost.localPort).thenReturn(5761)

    peerAccess.addAll(Seq("p1", "p2", "p3"))
    peerAccess.add("p1")
    peerAccess.add("p2")
    peerAccess.add("p3")
    peerAccess.add("p4")
    peerAccess.add("localhost:5761")
    peerAccess.add("128.33.45.55:5761")
    peerAccess.peers.asScala.toSeq shouldBe Seq("p1", "p2", "p3", "p4")
  }

}
