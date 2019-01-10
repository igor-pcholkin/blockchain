package core

import java.util.concurrent.ConcurrentHashMap

import messages.SignedStatementMessage

class StatementsCache {
  val statements = new ConcurrentHashMap[Statement, SignedStatementMessage]()

  def add(signedStatementMessage: SignedStatementMessage): Unit = {
    if (!contains(signedStatementMessage)) {
      statements.put(signedStatementMessage.statement, signedStatementMessage)
    }
  }

  def addAll(statements: Seq[SignedStatementMessage]): Unit = statements foreach add

  def contains(sighedStatementMessage: SignedStatementMessage): Boolean = {
    Option(statements.get(sighedStatementMessage.statement)).nonEmpty
  }
}
