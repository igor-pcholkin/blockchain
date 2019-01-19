package http

import java.io.ByteArrayInputStream
import java.net.URI

import com.sun.net.httpserver.HttpExchange
import core._
import json.FactJson._
import json.JsonSerializer
import keys.{KeysFileOps, KeysGenerator}
import messages.{NewBlockMessage, SignedStatementMessage}
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.mockito.Matchers
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.PeerAccess
import statements.RegisteredUser
import util.{DateTimeUtil, StringConverter}

class RegisterUserHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with DateTimeUtil with KeysGenerator with StringConverter {
  "RegisterUserHandler" should "create new fact (registered user) in a new block" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val privateKey = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw=="
    val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val userName = "Igor"

    val registerUserRequest =
      s"""{
         | "name": "$userName",
         | "email": "ipcholkin@gmail.com"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(registerUserRequest.getBytes)

    when(mockExchange.getRequestURI).thenReturn(new URI("/registerUser"))
    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(peerAccess.localHost).thenReturn(mockLocalHost)

    when(keysFileOps.isKeysDirExists("Riga", userName)).thenReturn(false)
    when(keysFileOps.getUserByKey("Riga", publicKey)).thenReturn(Some(userName))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", userName, "privateKey")).thenReturn(privateKey)
    when(keysFileOps.readKeyFromFile("Riga", userName, "publicKey")).thenReturn(publicKey)

    new RegisterUserHandler("Riga", mockBcHttpServer, keysFileOps, peerAccess, blockChain, statementsCache).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New fact has been created and added to blockchain."))
    verify(keysFileOps, times(1)).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"), Matchers.eq("privateKey"), Matchers.any[String])
    verify(keysFileOps, times(1)).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"), Matchers.eq("publicKey"), Matchers.any[String])
    verify(peerAccess, times(1)).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 2
    val lastBlock = blockChain.getLatestBlock
    val fact = deserialize(new String(lastBlock.data)).right.get
    val statement = fact.statement.asInstanceOf[RegisteredUser]
    statement shouldBe RegisteredUser("Igor", "ipcholkin@gmail.com", publicKey)
    fact.statementHash.toSeq shouldBe SHA256.hash(statement.dataToSign).toSeq

    val signer = new Signer(keysFileOps)
    val signature = fact.providedSignaturesForKeys.head._2
    val decodedSignature = base64StrToBytes(signature)
    signer.verify("Riga", userName, fact.statement.dataToSign, decodedSignature) shouldBe true

  }

  it should "refuse to register the same user twice" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val keysFileOps = mock[KeysFileOps]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val privateKey = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw=="
    val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val userName = "Igor"

    val existingStatement = RegisteredUser("Igor", "ipcholkin@gmail.com", publicKey)
    val signedStatement = SignedStatementMessage(existingStatement, Nil)
    val fact = Fact(signedStatement.statement, signedStatement.providedSignaturesForKeys)
    val serializedFact = JsonSerializer.serialize(fact).getBytes
    val block = blockChain.genNextBlock(serializedFact)
    blockChain.add(block)

    blockChain.size shouldBe 2

    val registerUserRequest =
      s"""{
         | "name": "$userName",
         | "email": "ipcholkin@gmail.com"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(registerUserRequest.getBytes)

    when(mockExchange.getRequestURI).thenReturn(new URI("/registerUser"))
    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(peerAccess.localHost).thenReturn(mockLocalHost)

    when(keysFileOps.isKeysDirExists("Riga", userName)).thenReturn(false)
    when(keysFileOps.getUserByKey("Riga", publicKey)).thenReturn(Some(userName))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", userName, "privateKey")).thenReturn(privateKey)
    when(keysFileOps.readKeyFromFile("Riga", userName, "publicKey")).thenReturn(publicKey)

    new RegisterUserHandler("Riga", mockBcHttpServer, keysFileOps, peerAccess, blockChain, statementsCache).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("Refused new block creation - existing fact."))
    verify(keysFileOps, times(1)).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"), Matchers.eq("privateKey"), Matchers.any[String])
    verify(keysFileOps, times(1)).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"), Matchers.eq("publicKey"), Matchers.any[String])
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, never).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 2
  }

  it should "refuse to register new user if request is missing a mandatory (name) field" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val peerAccess = mock[PeerAccess]
    val keysFileOps = mock[KeysFileOps]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache

    val registerUserRequest =
      s"""{
         | "email": "ipcholkin@gmail.com"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(registerUserRequest.getBytes)

    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    new RegisterUserHandler("Riga", mockBcHttpServer, keysFileOps, peerAccess, blockChain, statementsCache).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_BAD_REQUEST),
      Matchers.eq("\"name\" field in user registration is missing."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, never).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 1
  }

  it should "refuse to register a user if there are existing keys for user with the same name and no overwriteKeys or useExistingKeys is specified" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache
    val keysFileOps = mock[KeysFileOps]
    val userName = "Igor"

    val registerUserRequest =
      s"""{
         | "name": "$userName",
         | "email": "ipcholkin@gmail.com"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(registerUserRequest.getBytes)

    when(mockExchange.getRequestURI).thenReturn(new URI("/registerUser"))
    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(keysFileOps.isKeysDirExists("Riga", userName)).thenReturn(true)

    new RegisterUserHandler("Riga", mockBcHttpServer, keysFileOps, peerAccess, blockChain, statementsCache).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_BAD_REQUEST),
      Matchers.eq("Public or private key already exists, use overwriteKeys=true to overwrite, useExistingKeys=true to attach existing keys."))
    verify(peerAccess, never).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, never).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 1
  }

  it should "create new fact (registered user) in a new block overwriting existing keys for the user" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache
    val keysFileOps = mock[KeysFileOps]
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val privateKey = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw=="
    val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val userName = "Igor"

    val registerUserRequest =
      s"""{
         | "name": "$userName",
         | "email": "ipcholkin@gmail.com"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(registerUserRequest.getBytes)

    when(mockExchange.getRequestURI).thenReturn(new URI("/registerUser?overwriteKeys=true"))
    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(peerAccess.localHost).thenReturn(mockLocalHost)

    when(keysFileOps.isKeysDirExists("Riga", userName)).thenReturn(true)
    when(keysFileOps.getUserByKey("Riga", publicKey)).thenReturn(Some(userName))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", userName, "privateKey")).thenReturn(privateKey)
    when(keysFileOps.readKeyFromFile("Riga", userName, "publicKey")).thenReturn(publicKey)

    new RegisterUserHandler("Riga", mockBcHttpServer, keysFileOps, peerAccess, blockChain, statementsCache).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New fact has been created and added to blockchain."))
    verify(keysFileOps, times(1)).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"), Matchers.eq("privateKey"), Matchers.any[String])
    verify(keysFileOps, times(1)).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"), Matchers.eq("publicKey"), Matchers.any[String])
    verify(peerAccess, times(1)).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 2
    val lastBlock = blockChain.getLatestBlock
    val fact = deserialize(new String(lastBlock.data)).right.get
    val statement = fact.statement.asInstanceOf[RegisteredUser]
    statement shouldBe RegisteredUser("Igor", "ipcholkin@gmail.com", publicKey)

    val signer = new Signer(keysFileOps)
    val signature = fact.providedSignaturesForKeys.head._2
    val decodedSignature = base64StrToBytes(signature)
    signer.verify("Riga", userName, fact.statement.dataToSign, decodedSignature) shouldBe true

  }

  it should "create new fact (registered user) in a new block using existing keys for the user" in {
    val mockBcHttpServer = mock[BCHttpServer]
    val mockExchange = mock[HttpExchange]
    val mockLocalHost = mock[LocalHost]
    val peerAccess = mock[PeerAccess]
    val blockChain = new TestBlockChain
    val statementsCache = new StatementsCache
    val keysFileOps = mock[KeysFileOps]
    peerAccess.addAll(Seq("blabla.com", "another.com"))
    val privateKey = "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw=="
    val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val userName = "Igor"

    val registerUserRequest =
      s"""{
         | "name": "$userName",
         | "email": "ipcholkin@gmail.com"
         | }
      """.stripMargin
    val is = new ByteArrayInputStream(registerUserRequest.getBytes)

    when(mockExchange.getRequestURI).thenReturn(new URI("/registerUser?useExistingKeys=true"))
    when(mockExchange.getRequestMethod).thenReturn("POST")
    when(mockExchange.getRequestBody).thenReturn(is)

    when(peerAccess.localHost).thenReturn(mockLocalHost)

    when(keysFileOps.isKeysDirExists("Riga", userName)).thenReturn(true)
    when(keysFileOps.getUserByKey("Riga", publicKey)).thenReturn(Some(userName))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", userName, "privateKey")).thenReturn(privateKey)
    when(keysFileOps.readKeyFromFile("Riga", userName, "publicKey")).thenReturn(publicKey)

    new RegisterUserHandler("Riga", mockBcHttpServer, keysFileOps, peerAccess, blockChain, statementsCache).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange),
      Matchers.eq("New fact has been created and added to blockchain."))
    verify(keysFileOps, never).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"), Matchers.eq("privateKey"), Matchers.any[String])
    verify(keysFileOps, never).writeKeyToFile(Matchers.eq("Riga"), Matchers.eq("Igor"), Matchers.eq("publicKey"), Matchers.any[String])
    verify(peerAccess, times(1)).sendMsg(Matchers.any[NewBlockMessage])
    verify(blockChain.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.any[Block], Matchers.any[String])

    blockChain.size shouldBe 2
    val lastBlock = blockChain.getLatestBlock
    val fact = deserialize(new String(lastBlock.data)).right.get
    val statement = fact.statement.asInstanceOf[RegisteredUser]
    statement shouldBe RegisteredUser("Igor", "ipcholkin@gmail.com", publicKey)

    val signer = new Signer(keysFileOps)
    val signature = fact.providedSignaturesForKeys.head._2
    val decodedSignature = base64StrToBytes(signature)
    signer.verify("Riga", userName, fact.statement.dataToSign, decodedSignature) shouldBe true

  }
}
