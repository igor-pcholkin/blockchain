package util

import java.time.LocalDateTime

trait DateTimeUtil {
  def timeStampsAreWithin(ts1: LocalDateTime, ts2: LocalDateTime, maxDiffInMillis: Int): Boolean = {
    import java.time.Duration
    val dur = Duration.between(ts1, ts2)
    dur.toMillis < maxDiffInMillis
  }

  implicit val orderingForDateTime = new Ordering[LocalDateTime] {
    override def compare(date1: LocalDateTime, date2: LocalDateTime) = {
      date1.compareTo(date2)
    }
  }
}
