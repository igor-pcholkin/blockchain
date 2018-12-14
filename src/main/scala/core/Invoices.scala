package core

import java.util.concurrent.ConcurrentLinkedQueue

class Invoices {
  val invoices = new ConcurrentLinkedQueue[Invoice]()

  def add(invoice: Invoice) = {
    if (!invoices.contains(invoice)) {
      invoices.add(invoice)
    }
  }

  def addAll(invoices: Seq[Invoice]) = {
    invoices foreach (add(_))
  }

}
