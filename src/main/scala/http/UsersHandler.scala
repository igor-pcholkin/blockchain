package http

import java.io.IOException

import com.sun.net.httpserver.{HttpExchange, HttpHandler}
import core.Statement
import keys.KeysSerializator
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.slf4j.{Logger, LoggerFactory}
import statements.{ApprovedFact, RegisteredUser}
import util.{DateTimeUtil, HttpUtil, StringConverter}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class UsersHandler(hc: HttpContext) extends HttpHandler with StringConverter with DateTimeUtil with HttpUtil
  with KeysSerializator {

  val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override val keysFileOps = hc.keysFileOps

  @throws[IOException]
  def handle(exchange: HttpExchange): Unit = {

    val mayBeQuery = Option(exchange.getRequestURI.getQuery)
    (getRequestParam(mayBeQuery, "trusted") match {
      case Some(trusted) if trusted.toBoolean => readTrustedUsersWithErrorCheck(exchange,
        getRequestParam(mayBeQuery, "forUser"))
      case _ => Some(readAllUsers)
    }).map { users =>
      val userInURI = """/users/([A-Za-z\s]+)""".r
      exchange.getRequestURI.toString match {
        case userInURI(user) => filterByUser(user, users, exchange)
        case _ => users
      }
    } match {
      case Some(users) => hc.bcHttpServer.sendHttpResponse(exchange, combine(users))
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

  def readTrustedUsersWithErrorCheck(exchange: HttpExchange,
                                     mayBeForUser: Option[String]): Option[Seq[RegisteredUser]] = {
    mayBeForUser match {
      case Some(forUser) =>
        if (!keysFileOps.isKeysDirExists(hc.nodeName, forUser)) {
          hc.bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST, s"User ${forUser} doesn't exist")
          None
        } else {
          val approvedUsers = readApprovedUsers(forUser)
          Some(approvedUsers)
        }
      case None =>
        hc.bcHttpServer.sendHttpResponse(exchange, SC_BAD_REQUEST,
          "User name should be specified in request query (forUser)")
        None
    }
  }

  private def readApprovedUsers(forUser: String) = {
    val users = mutable.HashMap[String, RegisteredUser]()
    val userApprovals = ListBuffer[ApprovedFact]()

    val approverPublicKey = readPublicKey(hc.nodeName, forUser)
    forEachBlock { (statement, registeredUserFactHash) =>
      statement match {
        case user: RegisteredUser => users.put(registeredUserFactHash, user)
        case approvedFact: ApprovedFact if approvedFact.approverPublicKey == approverPublicKey =>
          userApprovals.append(approvedFact)
        case _ => // nothing to do
      }
      None
    }
    users.filter { case (registeredUserFactHash, registeredUser) =>
      registeredUser.publicKey == approverPublicKey || userApprovals.find(_.factHash == registeredUserFactHash).nonEmpty
    }.values.toSeq.sortBy(_.timestamp)
  }

  private def forEachBlock[T](processStatement: (Statement, String) => Option[T]) = {
    hc.blockChain.blocksFrom (0) flatMap { block =>
      hc.blockChain.extractFact (block) match {
        case Right (fact) => processStatement(fact.statement, block.factHash)
        case Left (_) =>
          logger.error (s"Cannot extract fact: ${new String (block.data)}")
          None
      }
    }
  }

  private def combine(users: Seq[RegisteredUser]) = users mkString "\n"
}

