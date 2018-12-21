package core

import java.util.concurrent.ConcurrentHashMap

import core.messages.InitPaymentMessage

class InitPayments {
  val initPayments = new ConcurrentHashMap[Int, InitPaymentMessage]()

  def add(initPayment: InitPaymentMessage) = {
    val initPaymentHashCode = initPayment.hashCode()
    if (!initPayments.contains(initPaymentHashCode)) {
      initPayments.put(initPaymentHashCode, initPayment)
    }
  }

  def addAll(initPayments: Seq[InitPaymentMessage]) = {
    initPayments foreach (add(_))
  }

}
