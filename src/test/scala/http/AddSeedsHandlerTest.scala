package http

import java.io.ByteArrayInputStream

import com.sun.net.httpserver.HttpExchange
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import ws.WSPeers

class AddSeedsHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar {
  "AddPeersHandler" should "add peers to blockchain" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]

    val peers = """blabla.com:6001, lala.com:6002, localhost:6001
      |localhost:6002,lala.com:6002""".stripMargin
    val is = new ByteArrayInputStream(peers.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    val wsPeers = new WSPeers
    new AddSeedsHandler(mockBcHttpServer, wsPeers).handle(mockExchange)

    wsPeers.peers.toArray shouldBe(Seq("blabla.com:6001", "lala.com:6002", "localhost:6001", "localhost:6002").toArray)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(201),
      Matchers.eq("New WS seeds have been added."))

  }
}
