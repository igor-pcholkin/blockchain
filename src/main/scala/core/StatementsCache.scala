package core

import java.util.concurrent.ConcurrentHashMap

import messages.SignedStatementMessage

class StatementsCache {
  val statements = new ConcurrentHashMap[SignedStatementMessage, SignedStatementMessage]()

  def add(statement: SignedStatementMessage): Unit = {
    if (!contains(statement)) {
      statements.put(statement, statement)
    }
  }

  def addAll(statements: Seq[SignedStatementMessage]): Unit = statements foreach add

  def contains(statement: SignedStatementMessage): Boolean = {
    Option(statements.get(statement)).nonEmpty
  }
}
