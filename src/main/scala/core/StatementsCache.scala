package core

import java.util.concurrent.ConcurrentHashMap

import messages.SignedStatementMessage

class StatementsCache {
  val statements = new ConcurrentHashMap[Int, SignedStatementMessage]()

  def add(statement: SignedStatementMessage): Unit = {
    val statementHashCode = statement.hashCode()
    if (!statements.contains(statementHashCode)) {
      statements.put(statementHashCode, statement)
    }
  }

  def addAll(statements: Seq[SignedStatementMessage]): Unit = statements foreach add
}
