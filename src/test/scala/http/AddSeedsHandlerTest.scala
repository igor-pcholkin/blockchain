package http

import java.io.ByteArrayInputStream
import java.net.{InetAddress, InetSocketAddress}

import com.sun.net.httpserver.HttpExchange
import core.messages.AddPeersMessage
import io.circe.Encoder
import org.apache.http.HttpStatus
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.PeerAccess

class AddSeedsHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar {
  "AddSeedsHandler" should "add seeds to blockchain" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]

    val seeds = """blabla.com:6001, lala.com:6002, localhost:6001, localhost:6002""".stripMargin
    val is = new ByteArrayInputStream(seeds.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)
    val localServerAddress = createLocalAddress()
    when(mockExchange.getLocalAddress).thenReturn(localServerAddress)

    val peerAccess = mock[PeerAccess]
    new AddSeedsHandler(mockBcHttpServer, peerAccess).handle(mockExchange)

    val peers = Seq("blabla.com:6001", "lala.com:6002", "localhost:6001", "localhost:6002")
    verify(peerAccess, times(1)).addAll(peers)
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(AddPeersMessage(peers :+ "123.233.22.44:1234")))(Matchers.any[Encoder[AddPeersMessage]])

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_CREATED),
      Matchers.eq("New seeds have been added."))

  }

  private def createLocalAddress() = {
    val inetAddress = InetAddress.getByName("123.233.22.44")
    new InetSocketAddress(inetAddress, 1234)
  }

}
