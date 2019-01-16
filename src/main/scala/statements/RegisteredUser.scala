package statements

import java.time.LocalDateTime

import core.Statement
import user.Photo

case class RegisteredUser(name: String, email: String, birthDate: Option[LocalDateTime] = None, phone: Option[String] = None,
                address: Option[String] = None, linkedInURL: Option[String] = None, facebookURL: Option[String] = None,
                githubURL: Option[String] = None, photo: Option[Photo] = None) extends Statement {

  override def dataToSign: Array[Byte] = {
    (name + email).getBytes
  }

  override val timestamp: LocalDateTime = LocalDateTime.now
}
