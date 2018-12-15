package core

import java.util.concurrent.ConcurrentLinkedQueue

class InitPayments {
  val initPayments = new ConcurrentLinkedQueue[InitPayment]()

  def add(initPayment: InitPayment) = {
    if (!initPayments.contains(initPayment)) {
      initPayments.add(initPayment)
    }
  }

  def addAll(initPayments: Seq[InitPayment]) = {
    initPayments foreach (add(_))
  }

}
