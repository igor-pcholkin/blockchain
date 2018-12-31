package core

import keys.KeysFileOps
import messages.SignedStatementMessage
import org.mockito.Matchers
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito.{never, verify, when}
import org.scalatest

class SignedStatementMessageTest extends FlatSpec with scalatest.Matchers with MockitoSugar {
  "Statement" should "sign itself by public key present locally which was not used yet" in {
    val publicKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val statement = createStatement(Seq("pubKA", publicKey), Seq(("pubKA", "sign")))
    val keysFileOps = mock[KeysFileOps]
    when(keysFileOps.getUserByKey("Riga", publicKey)).thenReturn(Some("Igor"))
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(publicKey)
    val signatures = statement.signByLocalPublicKeys("Riga", keysFileOps)
    signatures.size shouldBe 1
    signatures.head._1 shouldBe publicKey
  }

  it should "sign itself by both public key present locally which were not used yet" in {
    val publicKey1 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q=="
    val publicKey2 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val statement = createStatement(Seq(publicKey1, publicKey2), Nil)
    val keysFileOps = mock[KeysFileOps]
    when(keysFileOps.getUserByKey("Riga", publicKey1)).thenReturn(Some("John"))
    when(keysFileOps.getUserByKey("Riga", publicKey2)).thenReturn(Some("Igor"))
    when(keysFileOps.readKeyFromFile("Riga", "John", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCAimtA53n1kVMdG1OleLJtfbFnjr1zU5smd04yfbdWpUw==")
    when(keysFileOps.readKeyFromFile("Riga", "John", "publicKey")).thenReturn(publicKey1)
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "privateKey")).thenReturn("MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCC94HoY839pqOB/m2D00X4+8vsM6kzUby8gk7Eq8XVsgw==")
    when(keysFileOps.readKeyFromFile("Riga", "Igor", "publicKey")).thenReturn(publicKey2)
    val signatures = statement.signByLocalPublicKeys("Riga", keysFileOps)
    signatures.size shouldBe 2
    signatures.head._1 shouldBe publicKey1
    signatures(1)._1 shouldBe publicKey2
  }

  it should "sign itself by none public key present locally which were not used yet" in {
    val publicKey1 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEp0qOMxie16K1oArb+FGKB6YSbl+Hz3pLsVI4r6zWMXmtuD6QFZxGDhbvPO6c969SFEW5VmOSelb8ck+2TysK/Q=="
    val publicKey2 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEDibd8O5I928ZnTU7RYTy6Od3K3SrGlC+V8lkMYrdJuzT9Ig/Iq8JciaukxCYmVSO1mZuC65xMkxSb5Q0rNZ8og=="
    val statement = createStatement(Seq(publicKey1, publicKey2), Seq(publicKey1 -> "sign1", publicKey2 -> "sign2"))
    val keysFileOps = mock[KeysFileOps]
    val signatures = statement.signByLocalPublicKeys("Riga", keysFileOps)
    signatures.size shouldBe 0
  }

  it should "correctly find local user with public key which has not signed the statement yet" in {
    val statement = createStatement(Seq(("pubKA", "sign")))

    val keysFileOps = mock[KeysFileOps]

    verify(keysFileOps, never).getUserByKey(Matchers.eq("Riga"), Matchers.eq("pubKA"))
    when(keysFileOps.getUserByKey("Riga", "pubKB")).thenReturn(Some("Igor"))

    statement.couldBeSignedByLocalPublicKey("Riga", keysFileOps) shouldBe true
  }

  it should "correctly find the first local user with public key which has not signed the statement yet" in {
    val statement = createStatement(Nil)

    val keysFileOps = mock[KeysFileOps]

    when(keysFileOps.getUserByKey("Riga", "pubKA")).thenReturn(Some("Igor"))
    verify(keysFileOps, never).getUserByKey(Matchers.eq("Riga"), Matchers.eq("pubKB"))

    statement.couldBeSignedByLocalPublicKey("Riga", keysFileOps) shouldBe true
  }

  it should "not find local user with public key which has not signed the statement yet" in {
    val statement = createStatement(Seq(("pubKA", "sign")))

    val keysFileOps = mock[KeysFileOps]

    verify(keysFileOps, never).getUserByKey(Matchers.eq("Riga"), Matchers.eq("pubKA"))
    when(keysFileOps.getUserByKey("Riga", "pubKB")).thenReturn(None)

    statement.couldBeSignedByLocalPublicKey("Riga", keysFileOps) shouldBe false
  }

  it should "correctly find the second local user with public key which has not signed the statement yet" in {
    val statement = createStatement(Nil)

    val keysFileOps = mock[KeysFileOps]

    when(keysFileOps.getUserByKey("Riga", "pubKA")).thenReturn(None)
    when(keysFileOps.getUserByKey("Riga", "pubKB")).thenReturn(Some("Igor"))

    statement.couldBeSignedByLocalPublicKey("Riga", keysFileOps) shouldBe true
  }

  def createStatement(signatures: Seq[(String, String)]): SignedStatementMessage = {
    val statement = TestStatement("a")
    SignedStatementMessage(statement, Seq("pubKA", "pubKB"), "localhost", signatures)
  }

  def createStatement(neededKeys: Seq[String], providedSignatures: Seq[(String, String)]): SignedStatementMessage = {
    val statement = TestStatement("b")
    SignedStatementMessage(statement, neededKeys, "localhost", providedSignatures)
  }

}
