package core

import java.util.concurrent.ConcurrentLinkedQueue

class InitPayments {
  val initPayments = new ConcurrentLinkedQueue[InitPayment]()

  def add(invoice: InitPayment) = {
    if (!initPayments.contains(invoice)) {
      initPayments.add(invoice)
    }
  }

  def addAll(invoices: Seq[InitPayment]) = {
    invoices foreach (add(_))
  }

}
