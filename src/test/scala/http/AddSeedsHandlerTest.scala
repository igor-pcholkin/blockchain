package http

import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentLinkedQueue

import com.sun.net.httpserver.HttpExchange
import core.TestBlockChain
import messages.{AddPeersMessage, PullNewsMessage}
import org.apache.http.HttpStatus
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.PeerAccess
import util.FileOps

import scala.collection.JavaConverters._

class AddSeedsHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar {
  "AddSeedsHandler" should "add seeds to blockchain" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val bc = new TestBlockChain

    val seeds = """blabla.com:6001, lala.com:6002, localhost:6001, localhost:6002""".stripMargin
    val is = new ByteArrayInputStream(seeds.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)
    when(mockLocalHost.localServerAddress).thenReturn("123.233.22.44:1234")
    when(mockBcHttpServer.localHost).thenReturn(mockLocalHost)

    val peerAccess = mock[PeerAccess]
    val fileOps = mock[FileOps]
    val peers = Seq("blabla.com:6001", "lala.com:6002", "localhost:6001", "localhost:6002")
    val peersAsQueue = new ConcurrentLinkedQueue[String]()
    peersAsQueue.addAll(peers.asJava)
    when(peerAccess.peers).thenReturn(peersAsQueue)

    when(peerAccess.localHost).thenReturn(mockLocalHost)

    new AddSeedsHandler(mockBcHttpServer, peerAccess, "Riga", fileOps, bc).handle(mockExchange)

    verify(peerAccess, times(1)).addAll(peers)
    verify(fileOps, times(1)).createDirIfNotExists(Matchers.eq("Riga"))
    verify(fileOps, times(1)).writeFile(Matchers.eq("Riga/config"), Matchers.anyString)
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(AddPeersMessage(peers)))
    verify(peerAccess, times(1)).sendMsg(Matchers.eq(PullNewsMessage(bc.chain.size)))

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(HttpStatus.SC_CREATED),
      Matchers.eq("New seeds have been added."))

  }

}
