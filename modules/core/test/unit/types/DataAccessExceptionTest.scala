package unit.types

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import types.DataAccessException

class DataAccessExceptionTest extends AnyWordSpec with Matchers:

  "DataAccessException" must {
    "store message and cause" in {
      val cause = new RuntimeException("root cause")
      val ex = DataAccessException("something failed", cause)
      ex.getMessage mustBe "something failed"
      ex.getCause mustBe cause
    }

    "be a RuntimeException" in {
      val ex = DataAccessException("err", new Exception("x"))
      ex mustBe a[RuntimeException]
    }
  }
