package messages

import core.Message

case class PullNewsMessage(fromBlockNo: Int) extends Message
