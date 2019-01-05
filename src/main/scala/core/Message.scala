package core

/** message is any service information that peers use and exchange each which other and requires serialization */
trait Message

/**
  * Message envelope allows to segregate fields which should not be considered a part of message,
  * e.g. sentFromIPAddress - in order not to process the same message several times when it comes from different nodes.
  */
case class MessageEnvelope(message: Message, sentFromIPAddress: String)

