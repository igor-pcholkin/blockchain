package http

import com.sun.net.httpserver.HttpExchange
import keys.KeysGenerator
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar

class NodeInfoHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with KeysGenerator {
  "NodeInfoHandler" should "respond with node info without keys when keys are not there yet" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]

    when(mockBcHttpServer.getKeys).thenReturn(None)

    new NodeInfoHandler("Riga", mockBcHttpServer).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendBytesToHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"""Node name: Riga
                     |Public key: (None)""".stripMargin.getBytes))
  }

  "NodeInfoHandler" should "respond with node info with public key when keys are attached to http node" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]

    val keysPair = generateKeyPair()
    when(mockBcHttpServer.getKeys).thenReturn(Some(keysPair))

    new NodeInfoHandler("Riga", mockBcHttpServer).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendBytesToHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"""Node name: Riga
                     |Public key: ${keysPair.getPublic}""".stripMargin.getBytes))
  }

}
