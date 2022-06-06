package util

import org.scalatest.FlatSpec
import org.scalatest.Matchers

class ArithmeticCalculatorTest extends FlatSpec with Matchers {

  "infix calculator" should
    "parse expression properly" in {
      val ref = Map(
        "N" -> 1f,
        "K" -> 2f,
        "A" -> 3f,
        "B" -> 4f)

      val calc = new InfixArithmeticCalculator(ref)
      calc.evaluate("2 + 2") shouldEqual Right(4f)
      calc.evaluate("2 + 2 + 9") shouldEqual Right(13f)
      calc.evaluate("2 * 5 + 2") shouldEqual Right(12f)
      calc.evaluate("(2 + 2 * 9) + ((3 + 3) / 2)") shouldEqual Right(23f)
      calc.evaluate("(1+2+3) * N") shouldEqual Right(6f)
      calc.evaluate("(1+2+3) * K") shouldEqual Right(12f)
      calc.evaluate("(1+2+3) * A") shouldEqual Right(18f)
      calc.evaluate("(1+2+3) * B") shouldEqual Right(24f)
    }

}