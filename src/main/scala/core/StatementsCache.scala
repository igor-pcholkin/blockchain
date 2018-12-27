package core

import java.util.concurrent.ConcurrentHashMap

class StatementsCache {
  val statements = new ConcurrentHashMap[Int, Statement]()

  def add(statement: Statement): Unit = {
    val statementHashCode = statement.hashCode()
    if (!statements.contains(statementHashCode)) {
      statements.put(statementHashCode, statement)
    }
  }

  def addAll(statements: Seq[Statement]): Unit = statements foreach add
}
