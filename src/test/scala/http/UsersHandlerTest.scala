package http

import java.time.LocalDateTime

import business.Money
import com.sun.net.httpserver.HttpExchange
import core.{BlockChain, Fact, TestBlockChain}
import keys.KeysFileOps
import messages.SignedStatementMessage
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import statements.{Payment, RegisteredUser}
import util.StringConverter
import json.JsonSerializer
import json.FactJson._

class UsersHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar with StringConverter {
  "UsersHandler" should "return all users on request" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val keysFileOps = mock[KeysFileOps]

    val publicKey1 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val publicKey2 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q=="

    val user1 = RegisteredUser("Igor", "ipcholkin@gmail.com", publicKey1)
    addUser(user1, blockChain)
    val factHash1 = blockChain.getLatestBlock.factHash

    blockChain.add(blockChain.genNextBlock("randomFact".getBytes))

    val user2 = RegisteredUser("John", "john@gmail.com", publicKey2)
    addUser(user2, blockChain)
    val factHash2 = blockChain.getLatestBlock.factHash

    blockChain.add(blockChain.genNextBlock("randomFact2".getBytes))

    addPayment(Payment.verifyAndCreate("Riga", publicKey1, publicKey2, Money("EUR", 2025), LocalDateTime.of(2018, 12, 1, 15, 0)).right.get, blockChain)

    addPayment(Payment.verifyAndCreate("Riga", publicKey1, publicKey2, Money("EUR", 3035), LocalDateTime.of(2019, 1, 6, 12, 5)).right.get, blockChain)

    blockChain.size shouldBe 7

    new UsersHandler(mockBcHttpServer, blockChain).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(
      s"""$user1
         |$user2"""
        .stripMargin))

  }

  def addUser(registeredUser: RegisteredUser, bc: BlockChain) = {
    val signedStatement = SignedStatementMessage(registeredUser, Nil)
    val fact = Fact(signedStatement.statement, Nil)
    val serializedFact = JsonSerializer.serialize(fact).getBytes
    val block = bc.genNextBlock(serializedFact)
    bc.add(block)
  }

  def addPayment(payment: Payment, bc: BlockChain) = {
    val signedStatement1 = SignedStatementMessage(payment, Nil)
    bc.addFactToNewBlock(signedStatement1)
    val paymentHash1 = bc.getLatestBlock.factHash
  }
}
