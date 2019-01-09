package messages

import core.Message

case class PullNewsMessage(fromBlockNo: Int, inReply: Boolean = false) extends Message
