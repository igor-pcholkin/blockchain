package core

abstract class Asset

case class Money(currency: String, amountInCents: Long) extends Asset

