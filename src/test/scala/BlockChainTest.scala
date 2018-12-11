import java.util.concurrent.{ConcurrentLinkedDeque, ConcurrentLinkedQueue}

import org.scalatest.{FlatSpec, Matchers}

class BlockChainTest extends FlatSpec with Matchers {
  val q = new ConcurrentLinkedDeque[Int]
  q.add(1)
  q.add(2)
  q.add(3)
  println(q.peekLast())
}
