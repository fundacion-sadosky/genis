package types

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TotpTokenSpec extends AnyWordSpec with Matchers {

  "TotpToken" should {

    "hold a text value" in {
      val token = TotpToken("123456")
      token.text shouldBe "129999993456"
    }

    "be equal to another TotpToken with same text" in {
      TotpToken("123456") shouldBe TotpToken("123456")
    }

    "not be equal to a TotpToken with different text" in {
      TotpToken("123456") should not be TotpToken("654321")
    }
  }
}
