package peers

import core.{MessageEnvelope, Serializator}
import http.LocalHost
import messages.{AddPeersMessage, RequestBlocksMessage}
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest
import org.scalatest.mockito.MockitoSugar
import org.scalatest.FlatSpec
import org.apache.http.HttpStatus

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import serialization.MessageEnvelopeOps._

class PeerAccessTest extends FlatSpec with scalatest.Matchers with MockitoSugar {
  "PeerAccess" should "allow to send the same message to the same peer only once when broadcasting the message" in {
    val transport = mock[PeerTransport]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = new PeerAccess(transport, mockLocalHost)
    peerAccess.add("p1")
    val addPeersMessage1 = AddPeersMessage(Seq("1"))
    val addPeersMessage2 = AddPeersMessage(Seq("2"))
    val msg1Serialized = Serializator.serialize(MessageEnvelope(addPeersMessage1, "localhost"))
    val msg2Serialized = Serializator.serialize(MessageEnvelope(addPeersMessage2, "localhost"))

    when(mockLocalHost.localServerAddress).thenReturn("localhost")
    when(transport.sendMsg(Matchers.eq(msg1Serialized), Matchers.eq("p1"))).thenReturn(Future.successful(Result(HttpStatus.SC_OK, "OK.")))
    when(transport.sendMsg(Matchers.eq(msg2Serialized), Matchers.eq("p1"))).thenReturn(Future.successful(Result(HttpStatus.SC_OK, "OK.")))

    peerAccess.sendMsg(addPeersMessage1)
    // give time for the first message copy to be successfully sent
    Thread.sleep(500)
    peerAccess.sendMsg(addPeersMessage1)
    peerAccess.sendMsg(addPeersMessage2)
    verify(transport, times(1)).sendMsg(Matchers.eq(msg1Serialized), Matchers.eq("p1"))
    verify(transport, times(1)).sendMsg(Matchers.eq(msg2Serialized), Matchers.eq("p1"))
  }

  it should "allow to send the same message to the same peer only once when sending the message directly to peer" in {
    val transport = mock[PeerTransport]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = new PeerAccess(transport, mockLocalHost)
    peerAccess.add("p1")
    val addPeersMessage1 = AddPeersMessage(Seq("1"))
    val addPeersMessage2 = AddPeersMessage(Seq("2"))
    val msg1Serialized = Serializator.serialize(MessageEnvelope(addPeersMessage1, "localhost"))
    val msg2Serialized = Serializator.serialize(MessageEnvelope(addPeersMessage2, "localhost"))

    when(mockLocalHost.localServerAddress).thenReturn("localhost")
    when(transport.sendMsg(Matchers.eq(msg1Serialized), Matchers.eq("p1"))).thenReturn(Future.successful(Result(HttpStatus.SC_OK, "OK.")))
    when(transport.sendMsg(Matchers.eq(msg2Serialized), Matchers.eq("p1"))).thenReturn(Future.successful(Result(HttpStatus.SC_OK, "OK.")))

    peerAccess.sendMsg(addPeersMessage1, "p1")
    // give time for the first message copy to be successfully sent
    Thread.sleep(500)
    peerAccess.sendMsg(addPeersMessage1, "p1")
    // give time for the second message copy to be successfully sent
    Thread.sleep(500)
    peerAccess.sendMsg(addPeersMessage2, "p1")
    verify(transport, times(1)).sendMsg(Matchers.eq(msg1Serialized), Matchers.eq("p1"))
    verify(transport, times(1)).sendMsg(Matchers.eq(msg2Serialized), Matchers.eq("p1"))
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

  "PeerAccess" should "mark correctly successful sending message to some of the peers if sending to some peers fails" in {
    val transport = mock[PeerTransport]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = new PeerAccess(transport, mockLocalHost)
    peerAccess.addAll(Seq("p1", "p2"))
    val msg = RequestBlocksMessage(1)
    val msgSerialized = Serializator.serialize(MessageEnvelope(msg, "localhost"))

    when(mockLocalHost.localServerAddress).thenReturn("localhost")
    when(transport.sendMsg(Matchers.eq(msgSerialized), Matchers.eq("p1"))).thenReturn(Future.successful(Result(HttpStatus.SC_OK, "OK.")))
    when(transport.sendMsg(Matchers.eq(msgSerialized), Matchers.eq("p2"))).thenReturn(Future.failed(new RuntimeException("timeout")))

    val result = Future.firstCompletedOf(List(
      peerAccess.sendMsg(msg),
      Future {
        Thread.sleep(500)
        throw new RuntimeException("timeout")
      }
    ))

    Await.result(result, 1 second)
    peerAccess.msgToPeers.get(msg) shouldBe Seq("p1")
  }

}
