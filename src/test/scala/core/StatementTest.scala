package core

import keys.KeysFileOps
import org.mockito.Matchers
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito.{never, verify, when}
import org.scalatest

class StatementTest extends FlatSpec with scalatest.Matchers with MockitoSugar {
  "Statement" should "correctly find local user with public key which has not signed the statement yet" in {
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


  def createStatement(signatures: Seq[(String, String)]) = {
    new Statement {
      override val publicKeysRequiredToSignEncoded = Seq("pubKA", "pubKB")

      override val providedSignaturesForKeys = signatures

      def dataToSign = ???

      def addSignature(publicKey: String, signature: String) = ???

    }
  }
}
