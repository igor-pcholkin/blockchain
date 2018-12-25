package http

import com.sun.net.httpserver.HttpExchange
import core.{BlockChain, TestBlockChain}
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar

class GetChainHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar {
  "GetChainHandler" should "dump blockchain on request" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]

    val bc = new TestBlockChain
    val serialized = bc.serialize
    new GetChainHandler(mockBcHttpServer, bc).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(serialized))

  }
}
