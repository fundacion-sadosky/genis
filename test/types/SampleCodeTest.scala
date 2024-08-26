package types

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.Matchers._

class SampleCodeTest extends FlatSpec with Matchers {

  "SampleCode" should
    "be created from a valid string" in {

      val sc = SampleCode("AR-C-SHDG-100")

      sc.text should equal("AR-C-SHDG-100")

    }

  "SampleCode" should
    "throw exception if string is invalid" in {

      val value = "A-C-SHDG-1"

      val thrown = the[IllegalArgumentException] thrownBy SampleCode(value)

      thrown.getMessage should equal(
        "Invalid string format for a %1s object, '%2s' does not match pattern /%3s/"
          .format(classOf[SampleCode].getName(), value, SampleCode.validationRe))

    }
}