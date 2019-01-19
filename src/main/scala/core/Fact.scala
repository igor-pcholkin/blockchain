package core

import util.StringConverter

/**
  * Fact is a statement signed by all users which are required to sign it.
  * Facts are stored in blockchain.
  */
case class Fact(statement: Statement, providedSignaturesForKeys: Seq[(String, String)]) {
  val statementHash: Array[Byte] = SHA256.hash(statement.dataToSign)
}