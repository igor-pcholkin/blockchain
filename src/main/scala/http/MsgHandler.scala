package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.{InitPaymentMessage, InitPayments, Signer}
import keys.{KeysFileOps, KeysSerializator}
import util.StringConverter

import scala.io.Source

class MsgHandler(bcHttpServer: BCHttpServer, initPayments: InitPayments, val keysFileOps: KeysFileOps) extends HttpHandler
  with StringConverter with KeysSerializator {
  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    if (exchange.getRequestMethod == "POST") {
      val msgAsString = Source.fromInputStream(exchange.getRequestBody).getLines().mkString("\n")
      val message = InitPaymentMessage.deserialize(msgAsString) match {
        case Right(initPaymentMessage) =>
          println(s"Deser msg: $initPaymentMessage")
          if (verifySignature(initPaymentMessage)) {
            initPayments.add(initPaymentMessage)
            bcHttpServer.sendHttpResponse(exchange, "Initial payment message verified.")
          } else {
            bcHttpServer.sendHttpResponse(exchange, 400, "Initial payment message validation failed.")
          }
        case Left(error) =>
          bcHttpServer.sendHttpResponse(exchange, 400, error.getMessage)

      }
    } else {
      bcHttpServer.sendHttpResponse(exchange, 400, "Invalid method, use POST")
    }
  }

  def verifySignature(initPaymentMessage: InitPaymentMessage) = {
    initPaymentMessage.encodedSignature match {
      case Some(encodedSignature) =>
        val decodedSignature = base64StrToBytes(encodedSignature)
        val decodedPublicKey = deserializePublic(initPaymentMessage.fromPublicKeyEncoded)
        Signer.verify(decodedSignature, initPaymentMessage.dataToSign, decodedPublicKey)
      case None => false
    }
  }
}