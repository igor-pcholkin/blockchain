package http

import com.sun.net.httpserver.HttpExchange
import core.{StatementsCache, TestBlockChain, TestStatement}
import keys.{KeysFileOps, KeysGenerator}
import messages.SignedStatementMessage
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.PeerAccess
import util.FileOps

class NodeInfoHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with KeysGenerator {
  "NodeInfoHandler" should "respond with node info" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = PeerAccess(mockLocalHost)
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache

    peerAccess.add("localhost:9001,localhost:9002")
    statementsCache.add(SignedStatementMessage(new TestStatement("1+1=2"), Nil))
    statementsCache.add(SignedStatementMessage(new TestStatement("a cow drinks milk"), Nil))
    blockChain.add(blockChain.genNextBlock("aaa".getBytes))
    blockChain.add(blockChain.genNextBlock("bbc".getBytes))

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      mock[KeysFileOps], mock[FileOps])
    new NodeInfoHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq(s"""Node name: Riga
                     |Peers: [localhost:9001,localhost:9002]
                     |Blocks in blockchain: 3
                     |Size of statements cache: 2""".stripMargin))
  }

}
