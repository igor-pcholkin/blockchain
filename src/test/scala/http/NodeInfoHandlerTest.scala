package http

import com.sun.net.httpserver.HttpExchange
import keys.KeysGenerator
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.PeerAccess

class NodeInfoHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with KeysGenerator {
  "NodeInfoHandler" should "respond with rudimentary node info" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val peerAccess = PeerAccess()

    new NodeInfoHandler("Riga", mockBcHttpServer, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"""Node name: Riga
                     |Peers: []""".stripMargin))
  }

  "NodeInfoHandler" should "respond with enhanced node info" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val peerAccess = PeerAccess()
    peerAccess.add("localhost:9001,localhost:9002")

    new NodeInfoHandler("Riga", mockBcHttpServer, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"""Node name: Riga
                     |Peers: [localhost:9001,localhost:9002]""".stripMargin))
  }

}
