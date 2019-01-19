package statements

import java.time.LocalDateTime

import core.Statement
import user.Photo

case class RegisteredUser(name: String, email: String, publicKey: String, birthDate: Option[LocalDateTime] = None, phone: Option[String] = None,
                address: Option[String] = None, linkedInURL: Option[String] = None, facebookURL: Option[String] = None,
                githubURL: Option[String] = None, photo: Option[Photo] = None,
                override val timestamp: LocalDateTime = LocalDateTime.now) extends Statement {

  override def dataToSign: Array[Byte] = {
    (name + email + publicKey + timestamp).getBytes
  }

  override def equals(another: scala.Any): Boolean = {
    if (!another.isInstanceOf[RegisteredUser]) {
      false
    } else {
      val anotherRegisteredUser = another.asInstanceOf[RegisteredUser]
      name == anotherRegisteredUser.name &&
      email == anotherRegisteredUser.email &&
      publicKey == anotherRegisteredUser.publicKey
    }
  }

  override def hashCode(): Int = (name + email + publicKey).hashCode
}
