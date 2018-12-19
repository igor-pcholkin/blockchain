package http

import java.net.URI
import java.security.KeyPair

import com.sun.net.httpserver.HttpExchange
import keys.KeysFileOps
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers
import org.mockito.Matchers._
import org.scalatest.FlatSpec

class GenKeysHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar {
  "GenKeysHandler" should "create new key pair, store it in local file system and attach to running http node" in {
    val mockExchange = mock[HttpExchange]
    val mockKeysFileOps = mock[KeysFileOps]
    val mockBcHttpServer = mock[BCHttpServer]
    when(mockExchange.getRequestURI).thenReturn(new URI("/genkeys"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockBcHttpServer.nonEmptyKeys).thenReturn(false)
    when(mockKeysFileOps.isKeysDirExists(any[String])).thenReturn(false)

    new GenKeysHandler("Riga", mockKeysFileOps, mockBcHttpServer).handle(mockExchange)

    verify(mockKeysFileOps, times(1)).isKeysDirExists(any[String])
    verify(mockKeysFileOps, times(1)).createKeysDir(any[String])
    verify(mockKeysFileOps, times(1)).writeKey(Matchers.eq("Riga/privateKey"), any[String])
    verify(mockKeysFileOps, times(1)).writeKey(Matchers.eq("Riga/publicKey"), any[String])
    verify(mockBcHttpServer, times(1)).setKeys(any[KeyPair])
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(201),
      Matchers.eq("New keys have been created"))
  }

  "GenKeysHandler" should "not create new key pair if it already exists" in {
    val mockExchange = mock[HttpExchange]
    val mockKeysFileOps = mock[KeysFileOps]
    val mockBcHttpServer = mock[BCHttpServer]
    when(mockExchange.getRequestURI).thenReturn(new URI("/genkeys"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockBcHttpServer.nonEmptyKeys).thenReturn(true)

    new GenKeysHandler("Riga", mockKeysFileOps, mockBcHttpServer).handle(mockExchange)

    verify(mockKeysFileOps, never).writeKey(Matchers.eq("Riga/privateKey"), any[String])
    verify(mockKeysFileOps, never).writeKey(Matchers.eq("Riga/publicKey"), any[String])
    verify(mockBcHttpServer, never).setKeys(any[KeyPair])
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(400),
      Matchers.eq("Public or private key already exists, use overwrite=true to overwrite"))
  }

  "GenKeysHandler" should """create new key pair, store it in local file system and attach to running http node
    |when keys already exist but overwrite=true is specified""" in {
    val mockExchange = mock[HttpExchange]
    val mockKeysFileOps = mock[KeysFileOps]
    val mockBcHttpServer = mock[BCHttpServer]
    when(mockExchange.getRequestURI).thenReturn(new URI("/genkeys?overwrite=true"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockBcHttpServer.nonEmptyKeys).thenReturn(true)
    when(mockKeysFileOps.isKeysDirExists(any[String])).thenReturn(false)

    new GenKeysHandler("Riga", mockKeysFileOps, mockBcHttpServer).handle(mockExchange)

    verify(mockKeysFileOps, times(1)).isKeysDirExists(any[String])
    verify(mockKeysFileOps, times(1)).createKeysDir(any[String])
    verify(mockKeysFileOps, times(1)).writeKey(Matchers.eq("Riga/privateKey"), any[String])
    verify(mockKeysFileOps, times(1)).writeKey(Matchers.eq("Riga/publicKey"), any[String])
    verify(mockBcHttpServer, times(1)).setKeys(any[KeyPair])
    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(201),
      Matchers.eq("New keys have been created"))
  }

}

