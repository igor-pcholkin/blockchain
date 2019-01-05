package messages

import core.Message

case class RequestBlocksMessage(fromBlockNo: Int) extends Message
