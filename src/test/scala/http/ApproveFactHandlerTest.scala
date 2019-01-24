package http

import java.io.ByteArrayInputStream
import java.net.URI

import com.sun.net.httpserver.HttpExchange
import core._
import json.FactJson._
import keys.{KeysFileOps, KeysGenerator}
import messages.{NewBlockMessage, SignedStatementMessage}
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.mockito.Matchers
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.PeerAccess
import statements.ApprovedFact
import util.{DateTimeUtil, FileOps, StringConverter}

class ApproveFactHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with DateTimeUtil
  with KeysGenerator with StringConverter {
  "ApproveFactHandler" should "create new fact (another fact approval) in a new block" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val keysFileOps = mock[KeysFileOps]
    val fileOps = mock[FileOps]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val privateKey = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw=="
    val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val userName = "Igor"

    val block1 = blockChain.genNextBlock("Fund transfer from A to B".getBytes)
    blockChain.add(block1)
    blockChain.size shouldBe 2

    val approveFactRequest =
      s"""{
         | "factHash": "${block1.factHash}",
         | "approverUserName": "$userName"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(approveFactRequest.getBytes)

    when(mockExchange.getRequestURI).thenReturn(new URI("/approveFact"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(peerAccess.localHost).thenReturn(mockLocalHost)

    when(keysFileOps.isKeysDirExists("Riga", userName)).thenReturn(true)
    when(keysFileOps.readKeyFromFile("Riga", userName, "privateKey")).thenReturn(privateKey)
    when(keysFileOps.readKeyFromFile("Riga", userName, "publicKey")).thenReturn(publicKey)
    when(keysFileOps.getUserByKey("Riga", publicKey)).thenReturn(Some(userName))

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, fileOps)
    new ApproveFactHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New fact has been created and added to blockchain."))
    verify(peerAccess, times(1)).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 3
    val lastBlock = blockChain.getLatestBlock
    val fact = deserialize(new String(lastBlock.data)).right.get
    val statement = fact.statement.asInstanceOf[ApprovedFact]
    statement shouldBe ApprovedFact(block1.factHash, publicKey)

    val signer = new Signer(keysFileOps)
    val signature = fact.providedSignaturesForKeys.head._2
    val decodedSignature = base64StrToBytes(signature)
    signer.verify("Riga", userName, fact.statement.dataToSign, decodedSignature) shouldBe true

  }

  it should "refuse to approve the same fact twice" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val keysFileOps = mock[KeysFileOps]
    val fileOps = mock[FileOps]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val privateKey = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw=="
    val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val userName = "Igor"

    val block1 = blockChain.genNextBlock("Fund transfer from A to B".getBytes)
    blockChain.add(block1)

    val existingStatement = ApprovedFact(block1.factHash, publicKey)
    val signedStatement = SignedStatementMessage(existingStatement, Nil)
    blockChain.addFactToNewBlock(signedStatement)

    blockChain.size shouldBe 3

    val approveFactRequest =
      s"""{
         | "factHash": "${block1.factHash}",
         | "approverUserName": "$userName"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(approveFactRequest.getBytes)

    when(mockExchange.getRequestURI).thenReturn(new URI("/approveFact"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(peerAccess.localHost).thenReturn(mockLocalHost)

    when(keysFileOps.isKeysDirExists("Riga", userName)).thenReturn(true)
    when(keysFileOps.readKeyFromFile("Riga", userName, "privateKey")).thenReturn(privateKey)
    when(keysFileOps.readKeyFromFile("Riga", userName, "publicKey")).thenReturn(publicKey)
    when(keysFileOps.getUserByKey("Riga", publicKey)).thenReturn(Some(userName))

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, fileOps)
    new ApproveFactHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Refused new block creation - existing fact."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 3
  }

  it should "refuse to approve a fact if request is missing factHash field" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val keysFileOps = mock[KeysFileOps]
    val fileOps = mock[FileOps]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val privateKey = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw=="
    val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val userName = "Igor"

    val block1 = blockChain.genNextBlock("Fund transfer from A to B".getBytes)
    blockChain.add(block1)

    blockChain.size shouldBe 2

    val approveFactRequest =
      s"""{
         | "approverUserName": "$userName"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(approveFactRequest.getBytes)

    when(mockExchange.getRequestURI).thenReturn(new URI("/approveFact"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess,
      keysFileOps, fileOps)
    new ApproveFactHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_BAD_REQUEST),
      Matchers.eq("\"factHash\" field in approve request is missing."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, never).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 2
  }

  it should "refuse to approve a fact if request is missing approverUserName field" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val keysFileOps = mock[KeysFileOps]
    val fileOps = mock[FileOps]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val privateKey = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw=="
    val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val userName = "Igor"

    val block1 = blockChain.genNextBlock("Fund transfer from A to B".getBytes)
    blockChain.add(block1)

    blockChain.size shouldBe 2

    val approveFactRequest =
      s"""{
         | "factHash": "${block1.factHash}"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(approveFactRequest.getBytes)

    when(mockExchange.getRequestURI).thenReturn(new URI("/approveFact"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps, fileOps)
    new ApproveFactHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_BAD_REQUEST),
      Matchers.eq("\"approverUserName\" field in approve request is missing."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, never).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 2
  }

  it should "refuse fact approval request if the referenced fact doesn't exist in blockchain" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val keysFileOps = mock[KeysFileOps]
    val fileOps = mock[FileOps]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val privateKey = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw=="
    val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val userName = "Igor"

    val approveFactRequest =
      s"""{
         | "factHash": "FO1gZu3Z+IxverXL/YPv1CdSGRtRzp+Ypj6x0tH5F48=",
         | "approverUserName": "$userName"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(approveFactRequest.getBytes)

    when(mockExchange.getRequestURI).thenReturn(new URI("/approveFact"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(keysFileOps.isKeysDirExists("Riga", userName)).thenReturn(true)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps, fileOps)
    new ApproveFactHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_BAD_REQUEST),
      Matchers.eq("Fact with given hash doesn't exist"))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, never).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 1
  }

  it should "refuse fact approval request if the referenced approver user doesn't exist on local host" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val keysFileOps = mock[KeysFileOps]
    val fileOps = mock[FileOps]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val privateKey = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw=="
    val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val userName = "Igor"

    val block1 = blockChain.genNextBlock("Fund transfer from A to B".getBytes)
    blockChain.add(block1)
    blockChain.size shouldBe 2

    val approveFactRequest =
      s"""{
         | "factHash": "${block1.factHash}",
         | "approverUserName": "$userName"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(approveFactRequest.getBytes)

    when(mockExchange.getRequestURI).thenReturn(new URI("/approveFact"))
    when(mockExchange.getRequestMethod).thenReturn("PUT")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(keysFileOps.isKeysDirExists("Riga", userName)).thenReturn(false)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, statementsCache, peerAccess, keysFileOps, fileOps)
    new ApproveFactHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_BAD_REQUEST),
      Matchers.eq(s"User $userName doesn't exist"))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, never).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 2
  }
}
