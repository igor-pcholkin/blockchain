package http

import java.io.IOException
import java.time.LocalDateTime

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import messages.SignedStatementMessage
import keys.{KeysGenerator, KeysSerializator}

import scala.io.Source
import io.circe.generic.auto._
import io.circe.parser._
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import statements.RegisteredUser
import user.Photo
import util.HttpUtil

case class RegisterUserRequest(name: String, email: String, birthDate: Option[LocalDateTime] = None,
     phone: Option[String] = None, address: Option[String] = None, linkedInURL: Option[String] = None,
     facebookURL: Option[String] = None, githubURL: Option[String] = None, photo: Option[Photo] = None)

class RegisterUserHandler(hc: HttpContext) extends HttpHandler with HttpUtil with KeysGenerator with KeysSerializator
  with MsgHandlerOps {

  override val blockChain = hc.blockChain
  override val bcHttpServer = hc.bcHttpServer
  override val statementsCache = hc.statementsCache
  override val peerAccess = hc.peerAccess
  override val keysFileOps = hc.keysFileOps

  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {
    withHttpMethod ("PUT", exchange, bcHttpServer) {
      val s = Source.fromInputStream(exchange.getRequestBody)
      val inputAsString = s.getLines.mkString
      s.close()
      decode[RegisterUserRequest](inputAsString) match {
        case Right(user) =>
          processUser(user, exchange)
        case Left(error) =>
          val correctedMessage = correctValidationError(exchange, error.getMessage) match {
            case Some(message) => message
            case None => error.getMessage
          }
          bcHttpServer.sendHttpResponse (exchange, SC_BAD_REQUEST, correctedMessage)
      }
    }
  }

  private def processUser(registerUserRequest: RegisterUserRequest, exchange: HttpExchange): Unit = {
    generateKeys(registerUserRequest.name, exchange) match {
      case Right(()) =>
        val publicKey = readPublicKey(hc.nodeName, registerUserRequest.name)
        val registeredUser = createRegisteredUser(registerUserRequest, publicKey)
        val signedStatement = SignedStatementMessage(registeredUser, Seq(publicKey), hc.nodeName, keysFileOps)
        processStatementAsFact(signedStatement, exchange)
      case Left(error) =>
        bcHttpServer.sendHttpResponse (exchange, SC_BAD_REQUEST, error)
    }
  }

  private def createRegisteredUser(ru: RegisterUserRequest, publicKey: String) = {
    RegisteredUser(ru.name, ru.email, publicKey, ru.birthDate, ru.phone, ru.address, ru.linkedInURL,
      ru.facebookURL, ru.githubURL, ru.photo)
  }

  private def generateKeys(userName: String, exchange: HttpExchange) = {
    val mayBeQuery = Option(exchange.getRequestURI.getQuery)
    val overwriteKeys = getRequestParam(mayBeQuery, "overwriteKeys").getOrElse("false").toBoolean
    val useExistingKeys = getRequestParam(mayBeQuery, "useExistingKeys").getOrElse("false").toBoolean
    val isUserKeysDirExists = keysFileOps.isKeysDirExists(hc.nodeName, userName)
    if (!isUserKeysDirExists || overwriteKeys) {
      val keyPair = generateKeyPair()
      writeKey(hc.nodeName, userName, keyPair.getPrivate)
      writeKey(hc.nodeName, userName, keyPair.getPublic)
      Right()
    } else if (isUserKeysDirExists && useExistingKeys) {
      Right()
    } else {
      Left(
        """Public or private key already exists, use overwriteKeys=true to overwrite, useExistingKeys=true to attach existing keys.""".stripMargin)
    }
  }

  private def correctValidationError(exchange: HttpExchange, error: String) = {
    Stream("publicKey", "name", "email").flatMap {
      checkField(_, error, exchange)
    }.find(_.nonEmpty)
  }

  private def checkField(fieldName: String, error: String, exchange: HttpExchange) = {
    if (error == s"Attempt to decode value on failed cursor: DownField($fieldName)") {
      Some(s""""$fieldName" field in user registration is missing.""")
    } else
      None
  }
}
