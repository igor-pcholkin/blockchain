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

    when(mockBcHttpServer.getKeys).thenReturn(None)

    new NodeInfoHandler("Riga", mockBcHttpServer, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"""Node name: Riga
                     |Public key: (None)
                     |WebSocket peers: []""".stripMargin))
  }

  "NodeInfoHandler" should "respond with enhanced node info" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val peerAccess = PeerAccess()
    peerAccess.add("localhost:9001,localhost:9002")

    val keysPair = generateKeyPair()
    when(mockBcHttpServer.getKeys).thenReturn(Some(keysPair))

    new NodeInfoHandler("Riga", mockBcHttpServer, peerAccess).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"""Node name: Riga
                     |Public key: ${keysPair.getPublic}
                     |WebSocket peers: [localhost:9001,localhost:9002]""".stripMargin))
  }

}
