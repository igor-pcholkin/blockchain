package util

import org.scalatest.{FlatSpec, Matchers}

class HttpUtilTest extends FlatSpec with Matchers with HttpUtil {
  "getRequestParam method" should "extract values for given http request parameters" in {
    val query = Some("userName=Igor&overwrite=true")
    getRequestParam(query, "userName") shouldBe Some("Igor")
    getRequestParam(query, "overwrite") shouldBe Some("true")
    getRequestParam(query, "blabla") shouldBe None
  }

  "getRequestParam method" should "return None if query is not specified" in {
    val query = None
    getRequestParam(query, "userName") shouldBe None
    getRequestParam(query, "overwrite") shouldBe None
    getRequestParam(query, "blabla") shouldBe None
  }
}
