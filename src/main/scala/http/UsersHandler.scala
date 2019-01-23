package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.{BlockChain, Statement}
import keys.{KeysFileOps, KeysSerializator}
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.slf4j.{Logger, LoggerFactory}
import statements.{ApprovedFact, RegisteredUser}
import util.{DateTimeUtil, HttpUtil, StringConverter}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class UsersHandler(nodeName: String, bcHttpServer: BCHttpServer, bc: BlockChain, val keysFileOps: KeysFileOps) extends HttpHandler with StringConverter
  with DateTimeUtil with HttpUtil with KeysSerializator {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {

    val mayBeQuery = Option(exchange.getRequestURI.getQuery)
    getRequestParam(mayBeQuery, "trusted") match {
      case Some(trusted) if trusted.toBoolean => respondWithTrustedUsers(exchange, getRequestParam(mayBeQuery, "forUser"))
      case _ => respondWithAllUsers(exchange)
    }

  }

  def respondWithAllUsers(exchange: HttpExchange) = {
    val users = forEachBlock { (statement, hash) =>
      statement match {
        case user: RegisteredUser => Some(user)
        case _ => None
      }
    }

    bcHttpServer.sendHttpResponse(exchange, combine(users))
  }

  def respondWithTrustedUsers(exchange: HttpExchange, mayBeForUser: Option[String]) = {
    mayBeForUser match {
      case Some(forUser) =>
        if (!keysFileOps.isKeysDirExists(nodeName, forUser)) {
          bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, s"User ${forUser} doesn't exist")
        } else {
          val approvedUsers = readApprovedUsers(forUser)
          bcHttpServer.sendHttpResponse(exchange, combine(approvedUsers))
        }
      case None =>
        bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "User name should be specified in request query (forUser)")
    }
  }

  private def readApprovedUsers(forUser: String) = {
    val users = mutable.HashMap[String, RegisteredUser]()
    val userApprovals = ListBuffer[ApprovedFact]()

    val approverPublicKey = readPublicKey(nodeName, forUser)
    forEachBlock { (statement, registeredUserFactHash) =>
      statement match {
        case user: RegisteredUser => users.put(registeredUserFactHash, user)
        case approvedFact: ApprovedFact if approvedFact.approverPublicKey == approverPublicKey => userApprovals.append(approvedFact)
        case _ => // none
      }
      None
    }
    users.filter { case (registeredUserFactHash, registeredUser) =>
      registeredUser.publicKey == approverPublicKey || userApprovals.find(_.factHash == registeredUserFactHash).nonEmpty
    }.values.toSeq.sortBy(_.timestamp)
  }

  private def forEachBlock[T](processStatement: (Statement, String) => Option[T]) = {
    bc.blocksFrom (0) flatMap { block =>
      bc.extractFact (block) match {
        case Right (fact) => processStatement(fact.statement, block.factHash)
        case Left (_) =>
          logger.error (s"Cannot extract fact: ${new String (block.data)}")
          None
      }
    }
  }

  private def combine(users: Seq[RegisteredUser]) = users mkString "\n"
}

