package http

import java.net.URI

import com.sun.net.httpserver.HttpExchange
import core.{BlockChain, StatementsCache, TestBlockChain}
import keys.KeysFileOps
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers
import org.mockito.Matchers._
import org.scalatest.FlatSpec
import org.apache.http.HttpStatus.{SC_BAD_REQUEST, SC_CREATED}
import peers.PeerAccess
import util.FileOps

class GenKeysHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar {
  "GenKeysHandler" should "create new key pair and store it in local file system for given user" in {
    val mockExchange = mock[HttpExchange]
    val mockKeysFileOps = mock[KeysFileOps]
    val mockFileOps = mock[FileOps]
    val mockPeerAccess = mock[PeerAccess]
    val mockBcHttpServer = mock[BCHttpServer]
    when(mockExchange.getRequestURI).thenReturn(new URI("/genkeys?userName=Igor"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockKeysFileOps.isKeysDirExists(any[String], any[String])).thenReturn(false)

    val httpContext = HttpContext("Riga", mockBcHttpServer, new TestBlockChain, new StatementsCache, mockPeerAccess,
      mockKeysFileOps, mockFileOps)
    new GenKeysHandler(httpContext).handle(mockExchange)

    verify(mockKeysFileOps, times(1)).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"),
      Matchers.eq("privateKey"), any[String])
    verify(mockKeysFileOps, times(1)).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"),
      Matchers.eq("publicKey"), any[String])
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_CREATED),
      Matchers.eq("New keys have been created"))
  }

  "GenKeysHandler" should "not create new key pair if it already exists for given user" in {
    val mockExchange = mock[HttpExchange]
    val mockKeysFileOps = mock[KeysFileOps]
    val mockBcHttpServer = mock[BCHttpServer]
    val mockFileOps = mock[FileOps]
    val mockPeerAccess = mock[PeerAccess]
    when(mockExchange.getRequestURI).thenReturn(new URI("/genkeys?userName=Igor"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockKeysFileOps.isKeysDirExists(any[String], any[String])).thenReturn(true)

    val httpContext = HttpContext("Riga", mockBcHttpServer, new TestBlockChain, new StatementsCache, mockPeerAccess,
      mockKeysFileOps, mockFileOps)
    new GenKeysHandler(httpContext).handle(mockExchange)

    verify(mockKeysFileOps, never).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"),
      Matchers.eq("privateKey"), any[String])
    verify(mockKeysFileOps, never).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"),
      Matchers.eq("publicKey"), any[String])
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_BAD_REQUEST),
      Matchers.eq("Public or private key already exists, use overwrite=true to overwrite"))
  }

  "GenKeysHandler" should """create new key pair and store it in local file system
    |when keys for give user already exist but overwrite=true is specified""" in {
    val mockExchange = mock[HttpExchange]
    val mockKeysFileOps = mock[KeysFileOps]
    val mockBcHttpServer = mock[BCHttpServer]
    val mockFileOps = mock[FileOps]
    val mockPeerAccess = mock[PeerAccess]
    when(mockExchange.getRequestURI).thenReturn(new URI("/genkeys?userName=Igor&overwrite=true"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockKeysFileOps.isKeysDirExists(any[String], any[String])).thenReturn(true)

    val httpContext = HttpContext("Riga", mockBcHttpServer, new TestBlockChain, new StatementsCache, mockPeerAccess,
      mockKeysFileOps, mockFileOps)
    new GenKeysHandler(httpContext).handle(mockExchange)

    verify(mockKeysFileOps, times(1)).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"),
      Matchers.eq("privateKey"), any[String])
    verify(mockKeysFileOps, times(1)).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"),
      Matchers.eq("publicKey"), any[String])
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_CREATED),
      Matchers.eq("New keys have been created"))
  }

  "GenKeysHandler" should "not create new key pair if user name is not specified in http request" in {
    val mockExchange = mock[HttpExchange]
    val mockKeysFileOps = mock[KeysFileOps]
    val mockBcHttpServer = mock[BCHttpServer]
    val mockFileOps = mock[FileOps]
    val mockPeerAccess = mock[PeerAccess]
    when(mockExchange.getRequestURI).thenReturn(new URI("/genkeys"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")

    val httpContext = HttpContext("Riga", mockBcHttpServer, new TestBlockChain, new StatementsCache, mockPeerAccess,
      mockKeysFileOps, mockFileOps)
    new GenKeysHandler(httpContext).handle(mockExchange)

    verify(mockKeysFileOps, never).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"),
      Matchers.eq("privateKey"), any[String])
    verify(mockKeysFileOps, never).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"),
      Matchers.eq("publicKey"), any[String])
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_BAD_REQUEST),
      Matchers.eq("User name should be specified in request query (userName)"))
  }

}

