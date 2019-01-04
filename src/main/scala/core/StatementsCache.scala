package core

import java.util.concurrent.ConcurrentHashMap

import messages.SignedStatementMessage

class StatementsCache {
  val statements = new ConcurrentHashMap[Int, SignedStatementMessage]()

  def add(statement: SignedStatementMessage): Unit = {
    if (!contains(statement)) {
      val statementHashCode = statement.hashCode()
      statements.put(statementHashCode, statement)
    }
  }

  def addAll(statements: Seq[SignedStatementMessage]): Unit = statements foreach add

  def contains(statement: SignedStatementMessage): Boolean = {
    val statementHashCode = statement.hashCode()
    Option(statements.get(statementHashCode)).nonEmpty
  }
}
