package statements

import java.time.LocalDateTime

import core.Statement
import user.Photo

case class ApprovedFact(factHash: String, approverPublicKey: String,
                        override val timestamp: LocalDateTime = LocalDateTime.now) extends Statement {

  override def dataToSign: Array[Byte] = {
    (factHash + timestamp).getBytes
  }

  override def equals(another: scala.Any): Boolean = {
    if (!another.isInstanceOf[ApprovedFact]) {
      false
    } else {
      val anotherApprovedFact = another.asInstanceOf[ApprovedFact]
      factHash == anotherApprovedFact.factHash &&
      approverPublicKey == anotherApprovedFact.approverPublicKey
    }
  }

  override def hashCode(): Int = (factHash + approverPublicKey).hashCode
}
