package business

import java.math.MathContext

abstract class Asset

case class Money(currency: String, amountInCents: Long) extends Asset {
  override def toString = {
    val correctAmount = BigDecimal(amountInCents).setScale(2) / 100
    s"$currency$correctAmount"
  }
}

