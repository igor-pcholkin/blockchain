package http

import java.net.URI
import java.time.LocalDateTime

import business.Money
import com.sun.net.httpserver.HttpExchange
import core.{BlockChain, Statement, StatementsCache, TestBlockChain}
import keys.KeysFileOps
import messages.SignedStatementMessage
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import peers.PeerAccess
import statements.{ApprovedFact, Payment, RegisteredUser}
import util.{FileOps, StringConverter}

class UsersHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with StringConverter {
  "UsersHandler" should "return all users on request" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val keysFileOps = mock[KeysFileOps]

    val (user1, user2, user3) = addTestData(blockChain)
    blockChain.size shouldBe 9

    when(mockExchange.getRequestURI).thenReturn(new URI("/users"))

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, new StatementsCache, mock[PeerAccess],
      keysFileOps, mock[FileOps])
    new UsersHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(
      s"""$user1
         |$user2
         |$user3"""
        .stripMargin))

  }

  it should "return only trusted users for given user" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val keysFileOps = mock[KeysFileOps]

    val publicKey2 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q=="

    val (user1, user2, user3) = addTestData(blockChain)
    blockChain.size shouldBe 9

    when(mockExchange.getRequestURI).thenReturn(new URI("/users?trusted=true&forUser=John"))
    when(keysFileOps.isKeysDirExists("Riga", "John")).thenReturn(true)
    when(keysFileOps.readKeyFromFile("Riga", "John", "publicKey")).thenReturn(publicKey2)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, new StatementsCache, mock[PeerAccess],
      keysFileOps, mock[FileOps])
    new UsersHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(
      s"""$user1
         |$user2"""
        .stripMargin))

  }

  it should "refuse request when specifying trusted=true without forUser request parameter" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val keysFileOps = mock[KeysFileOps]

    val (user1, user2, user3) = addTestData(blockChain)
    blockChain.size shouldBe 9

    when(mockExchange.getRequestURI).thenReturn(new URI("/users?trusted=true"))
    when(keysFileOps.isKeysDirExists("Riga", "John")).thenReturn(false)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, new StatementsCache, mock[PeerAccess],
      keysFileOps, mock[FileOps])
    new UsersHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_BAD_REQUEST),
      Matchers.eq("User name should be specified in request query (forUser)"))

  }

  it should "refuse request when referencing user with non-existing keys" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val keysFileOps = mock[KeysFileOps]

    val (user1, user2, user3) = addTestData(blockChain)
    blockChain.size shouldBe 9

    when(mockExchange.getRequestURI).thenReturn(new URI("/users?trusted=true&forUser=John"))
    when(keysFileOps.isKeysDirExists("Riga", "John")).thenReturn(false)

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, new StatementsCache, mock[PeerAccess],
      keysFileOps, mock[FileOps])
    new UsersHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(SC_BAD_REQUEST),
      Matchers.eq("User John doesn't exist"))

  }

  it should "find existing user by name" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val keysFileOps = mock[KeysFileOps]

    val (user1, user2, user3) = addTestData(blockChain)
    blockChain.size shouldBe 9

    when(mockExchange.getRequestURI).thenReturn(new URI("/users/Strauss"))

    val httpContext = HttpContext("Riga", mockBcHttpServer, blockChain, new StatementsCache, mock[PeerAccess],
      keysFileOps, mock[FileOps])
    new UsersHandler(httpContext).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(user3.toString))
  }

  def addTestData(blockChain: BlockChain) = {
    val publicKey1 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val publicKey2 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q=="
    val publicKey3 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEHT4R4zS0wZlpfUwrocaNM1vYHhjXDE1lZ5vKzaZFEjcN2GH2SRdFm/PmNlcXpEt7g8/FQdgMVA46cWbHUZDQsg=="

    val user1 = RegisteredUser("Igor", "ipcholkin@gmail.com", publicKey1)
    addSignedFact(user1, blockChain)
    val factHash1 = blockChain.getLatestBlock.factHash

    blockChain.add(blockChain.genNextBlock("randomFact".getBytes))

    val user2 = RegisteredUser("John", "john@gmail.com", publicKey2)
    addSignedFact(user2, blockChain)
    val factHash2 = blockChain.getLatestBlock.factHash

    blockChain.add(blockChain.genNextBlock("randomFact2".getBytes))

    addSignedFact(Payment.verifyAndCreate("Riga", publicKey1, publicKey2, Money("EUR", 2025),
      LocalDateTime.of(2018, 12, 1, 15, 0)).right.get, blockChain)

    addSignedFact(Payment.verifyAndCreate("Riga", publicKey1, publicKey2, Money("EUR", 3035),
      LocalDateTime.of(2019, 1, 6, 12, 5)).right.get, blockChain)

    val user3 = RegisteredUser("Levi Strauss", "levis@gmail.com", publicKey3)
    addSignedFact(user3, blockChain)
    val factHash3 = blockChain.getLatestBlock.factHash

    val approvedUser = ApprovedFact(factHash1, publicKey2)
    addSignedFact(approvedUser, blockChain)

    (user1, user2, user3)
  }


  def addSignedFact(statement: Statement, bc: BlockChain) = {
    val signedStatement1 = SignedStatementMessage(statement, Nil)
    bc.addFactToNewBlock(signedStatement1)
  }
}
