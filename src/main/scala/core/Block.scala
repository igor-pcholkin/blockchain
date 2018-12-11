package core

import java.time.LocalDateTime

case class Block(index: Int, prevHash: Array[Byte], timestamp: LocalDateTime, data: Array[Byte])
