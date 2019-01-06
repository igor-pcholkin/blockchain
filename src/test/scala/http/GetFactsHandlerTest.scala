package http

import java.time.LocalDateTime

import business.Money
import com.sun.net.httpserver.HttpExchange
import core.TestBlockChain
import keys.KeysFileOps
import messages.SignedStatementMessage
import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import statements.Payment

class GetFactsHandlerTest extends FlatSpec with org.scalatest.Matchers with MockitoSugar {
  "GetFactsHandler" should "return facts on request" in {
    val mockExchange = mock[HttpExchange]
    val mockBcHttpServer = mock[BCHttpServer]
    val blockChain = new TestBlockChain
    val keysFileOps = mock[KeysFileOps]

    val fromPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val toPublicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q=="

    when(keysFileOps.getUserByKey("Riga", fromPublicKey)).thenReturn(Some("Igor"))
    // needed to sign payment request message by public key of creator
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(fromPublicKey)
    // whether payment transaction could be created and signed
    when(keysFileOps.getUserByKey("Riga", toPublicKey)).thenReturn(Some("John"))
    // needed to sign payment transaction by public key of local node's owner
    when(keysFileOps.readKeyFromFile("Riga", "John", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAimtA53n1kVMdG1OleLJtfbFnjr1zU5smd04yfbdWpUw==")
    when(keysFileOps.readKeyFromFile("Riga", "John", "publicKey")).thenReturn(toPublicKey)

    val payment1 = Payment.verifyAndCreate("Riga", fromPublicKey, toPublicKey, Money("EUR", 2025), LocalDateTime.of(2018, 12, 1, 15, 0)).right.get
    val signedStatement1 = SignedStatementMessage(payment1, Seq(fromPublicKey, toPublicKey), "Riga", keysFileOps)
    blockChain.addFactToNewBlock(signedStatement1)

    val payment2 = Payment.verifyAndCreate("Riga", toPublicKey, fromPublicKey, Money("EUR", 3035), LocalDateTime.of(2019, 1, 6, 12, 5)).right.get
    val signedStatement2 = SignedStatementMessage(payment2, Seq(toPublicKey, fromPublicKey), "Riga", keysFileOps)
    blockChain.addFactToNewBlock(signedStatement2)

    new GetFactsHandler(mockBcHttpServer, blockChain).handle(mockExchange)

    verify(mockBcHttpServer, times(1)).sendHttpResponse(Matchers.eq(mockExchange), Matchers.eq(
      s"""Payment(Riga,MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==,MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q==,EUR20.25,2018-12-01T15:00)
         |Payment(Riga,MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q==,MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og==,EUR30.35,2019-01-06T12:05)"""
        .stripMargin))

  }
}
