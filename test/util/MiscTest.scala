package util

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.Matchers._
import scala.io.Source

class MiscTest extends FlatSpec with Matchers {

  val lessThan = (x: Int, y: Int) => x < y

  "test injection" should
    "be ok #1" in {
      val a = List(1, 2, 3, 4, 5)
      val b = List(2, 3, 4, 5, 6)

      val result = Misc.existsInjection(a, b, lessThan)

      result should be(true)

    }

  it should "be ok #2" in {
    val a = List(1, 2, 3, 4, 5)
    val b = List(8, 5, 2, 3, 4)

    val result = Misc.existsInjection(a, b, lessThan)

    result should be(true)

  }

  it should "be ok #3" in {
    val a = List(2, 2)
    val b = List(2, 2)

    val result = Misc.existsInjection(a, b, (x: Int, y: Int) => x == y)

    result should be(true)

  }

  it should "fail #1" in {
    val a = List(1, 2, 3, 4, 5)
    val b = List(1, 2, 3, 4, 5)

    val result = Misc.existsInjection(a, b, lessThan)

    result should be(false)

  }
}