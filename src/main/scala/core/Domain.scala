package core

import java.util.Currency

abstract class Asset

case class Money(currency: String, amountInCents: Long) extends Asset

