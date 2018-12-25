package peers

import messages.AddPeersMessage
import io.circe.Encoder
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify}
import org.scalatest
import org.scalatest.mockito.MockitoSugar
import org.scalatest.FlatSpec
import io.circe.generic.auto._

class PeerAccessTest extends FlatSpec with scalatest.Matchers with MockitoSugar {
  "PeerAccess" should "allow to send the same message to the same peer only once" in {
    val transport = mock[PeerTransport]
    val peerAccess = new PeerAccess(transport)
    peerAccess.add("p1")
    val addPeersMessage1 = AddPeersMessage(Seq("1"))
    val addPeersMessage2 = AddPeersMessage(Seq("2"))
    peerAccess.sendMsg(addPeersMessage1)
    peerAccess.sendMsg(addPeersMessage1)
    peerAccess.sendMsg(addPeersMessage2)
    verify(transport, times(1)).sendMsg(Matchers.eq(addPeersMessage1), Matchers.eq(Seq("p1")))(Matchers.any[Encoder[AddPeersMessage]])
    verify(transport, times(1)).sendMsg(Matchers.eq(addPeersMessage2), Matchers.eq(Seq("p1")))(Matchers.any[Encoder[AddPeersMessage]])
  }
}
