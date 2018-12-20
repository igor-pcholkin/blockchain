package http

import java.io.ByteArrayInputStream

import com.sun.net.httpserver.HttpExchange
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

    val seeds = """blabla.com:6001, lala.com:6002, localhost:6001
      |localhost:6002,lala.com:6002""".stripMargin
    val is = new ByteArrayInputStream(seeds.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    val peerAccess = PeerAccess()
    new AddSeedsHandler(mockBcHttpServer, peerAccess).handle(mockExchange)

    peerAccess.peers.toArray shouldBe(Seq("blabla.com:6001", "lala.com:6002", "localhost:6001", "localhost:6002").toArray)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_CREATED),
      Matchers.eq("New seeds have been added."))

  }
}
