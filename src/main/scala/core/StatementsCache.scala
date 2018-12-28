package core

import java.util.concurrent.ConcurrentHashMap

class StatementsCache {
  val statements = new ConcurrentHashMap[Int, SignedStatement]()

  def add(statement: SignedStatement): Unit = {
    val statementHashCode = statement.hashCode()
    if (!statements.contains(statementHashCode)) {
      statements.put(statementHashCode, statement)
    }
  }

  def addAll(statements: Seq[SignedStatement]): Unit = statements foreach add
}
