package core

import java.time.LocalDateTime

import business.Money
import io.circe.generic.auto._
import keys.KeysGenerator
import org.scalatest
import org.scalatest.mockito.MockitoSugar
import org.scalatest.FlatSpec
import json.JsonSerializer
import statements.Payment

class PaymentTest extends FlatSpec with scalatest.Matchers with MockitoSugar with KeysGenerator {
  "The test" should "create successfully payment statement and serialize it to json" in {
    val payment = Payment.verifyAndCreate("Riga", "(fromPublicKey)", "(toPublicKey)", Money("EUR", 2025), LocalDateTime.of(2019, 1, 1, 12, 0)).right.get

    val json = JsonSerializer.serialize(payment)
    json shouldBe s"""{"createdByNode":"Riga","fromPublicKeyEncoded":"(fromPublicKey)","toPublicKeyEncoded":"(toPublicKey)","money":{"currency":"EUR","amountInCents":2025},"timestamp":"2019-01-01T12:00:00"}"""
  }

  "Payment object" should "not create payment statement if user signing the message is the same as receiver of the payment" in {
    Payment.verifyAndCreate("Riga", "(fromPublicKey)", "(fromPublicKey)", Money("EUR", 2025),
      LocalDateTime.of(2019, 1, 1, 12, 0)) shouldBe Left("Sender and receiver of payment can't be the same person")
  }

}
