package core

import java.util.Currency

abstract class Asset

case class Money(currency: Currency, amountInCents: Long) extends Asset

