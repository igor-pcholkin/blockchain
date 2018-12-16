package core

import java.util.concurrent.ConcurrentLinkedQueue

class InitPayments {
  val initPayments = new ConcurrentLinkedQueue[InitPaymentMessage]()

  def add(initPayment: InitPaymentMessage) = {
    if (!initPayments.contains(initPayment)) {
      initPayments.add(initPayment)
    }
  }

  def addAll(initPayments: Seq[InitPaymentMessage]) = {
    initPayments foreach (add(_))
  }

}
