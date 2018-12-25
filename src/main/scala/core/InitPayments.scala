package core

import java.util.concurrent.ConcurrentHashMap

import messages.InitPaymentMessage

class InitPayments {
  val initPayments = new ConcurrentHashMap[Int, InitPaymentMessage]()

  def add(initPayment: InitPaymentMessage): Unit = {
    val initPaymentHashCode = initPayment.hashCode()
    if (!initPayments.contains(initPaymentHashCode)) {
      initPayments.put(initPaymentHashCode, initPayment)
    }
  }

  def addAll(initPayments: Seq[InitPaymentMessage]): Unit = {
    initPayments foreach add
  }

}
