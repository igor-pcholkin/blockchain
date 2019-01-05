package messages

import core.Message

case class AddPeersMessage(peers: Seq[String]) extends Message
