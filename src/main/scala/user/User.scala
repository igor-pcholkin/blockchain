package user

import java.time.LocalDateTime

case class Photo(data: Array[Byte], contentType: String, name: Option[String])

case class User(name: String, email: String, birthDate: Option[LocalDateTime] = None, phone: Option[String] = None,
                address: Option[String] = None, linkedInURL: Option[String] = None, facebookURL: Option[String] = None,
                githubURL: Option[String] = None, photo: Option[Photo] = None)
