package business

import org.scalatest
import org.scalatest.FlatSpec

class MoneyTest extends FlatSpec with scalatest.Matchers {
  "toString" should "produce representation with 2 decimal digits after point" in {
    Money("EUR", 2025).toString shouldBe "EUR20.25"
    Money("EUR", 2020).toString shouldBe "EUR20.20"
  }
}
