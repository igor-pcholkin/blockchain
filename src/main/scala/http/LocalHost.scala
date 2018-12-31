package http

import java.net.{InetSocketAddress, Socket}

case class LocalHost(localPort: Int) {
  lazy val localServerAddress: String = {
    // ugly, but it works... a better way?
    val socket = new Socket()
    socket.connect(new InetSocketAddress("google.com", 80))
    socket.getLocalAddress.toString.split("/")(1) + ":" + localPort
  }
}
