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
    (getRequestParam(mayBeQuery, "trusted") match {
      case Some(trusted) if trusted.toBoolean => readTrustedUsersWithErrorCheck(exchange, getRequestParam(mayBeQuery, "forUser"))
      case _ => Some(readAllUsers)
    }).map { users =>
      val userInURI = """/users/([A-Za-z\s]+)""".r
      exchange.getRequestURI.toString match {
        case userInURI(user) => filterByUser(user, users, exchange)
        case _ => users
      }
    } match {
      case Some(users) => bcHttpServer.sendHttpResponse(exchange, combine(users))
      case _ => // nothing, error returned
    }
  }

  private def filterByUser(user: String, users: Seq[RegisteredUser], exchange: HttpExchange): Seq[RegisteredUser] = {
    users.filter(_.name.contains(user))
  }

  private def readAllUsers: Seq[RegisteredUser] = forEachBlock  { (statement, hash) =>
    statement match {
      case user: RegisteredUser => Some(user)
      case _ => None
    }
  }

  def readTrustedUsersWithErrorCheck(exchange: HttpExchange, mayBeForUser: Option[String]): Option[Seq[RegisteredUser]] = {
    mayBeForUser match {
      case Some(forUser) =>
        if (!keysFileOps.isKeysDirExists(nodeName, forUser)) {
          bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, s"User ${forUser} doesn't exist")
          None
        } else {
          val approvedUsers = readApprovedUsers(forUser)
          Some(approvedUsers)
        }
      case None =>
        bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, "User name should be specified in request query (forUser)")
        None
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
        case _ => // nothing to do
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

