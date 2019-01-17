package core

import java.util.concurrent.ConcurrentHashMap

import messages.SignedStatementMessage
import scala.collection.JavaConverters._

class StatementsCache {
  private val statements = new ConcurrentHashMap[Statement, SignedStatementMessage]()

  def add(signedStatementMessage: SignedStatementMessage): Unit = {
    if (!contains(signedStatementMessage)) {
      statements.put(signedStatementMessage.statement, signedStatementMessage)
    }
  }

  def remove(statement: Statement): Unit = {
    statements.remove(statement)
  }

  def addAll(statements: Seq[SignedStatementMessage]): Unit = statements foreach add

  def contains(sighedStatementMessage: SignedStatementMessage): Boolean = {
    Option(statements.get(sighedStatementMessage.statement)).nonEmpty
  }

  def allStatementMessages = statements.values.asScala

  def size = statements.size
}
