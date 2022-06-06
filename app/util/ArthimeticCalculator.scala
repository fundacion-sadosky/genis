package util

import scala.util.parsing.combinator.JavaTokenParsers

abstract class ArithmeticCalculator {
  def evaluate(text: String): Either[String, Float]
}

class InfixArithmeticCalculator(constMap: Map[String, Float]) extends ArithmeticCalculator {

  private object parser extends JavaTokenParsers {

    val operate = (operator: String, a: Float, b: Float) => operator match {
      case "+" => a + b
      case "-" => a - b
      case "*" => a * b
      case "/" => a / b
    }

    val fold: PartialFunction[~[Float, List[~[String, Float]]], Float] = {
      case ~(value, list) =>
        val l = list map { case ~(a, b) => (a, b) }
        l.foldLeft(value) {
          case (left, (operator, right)) => operate(operator, left, right)
        }
    }

    def expr: Parser[Float] = term ~ rep("+" ~ term | "-" ~ term) ^^ fold
    def term: Parser[Float] = factor ~ rep("*" ~ factor | "/" ~ factor) ^^ fold
    def factor: Parser[Float] = floatingPointNumber ^^ (_.toFloat) | const | "(" ~> expr <~ ")"
    def const: Parser[Float] = constMap.keySet.mkString("|").r ^^ (constMap(_))

    def toEither[T](result: ParseResult[T]): Either[String, T] = result match {
      case Success(value, _) => Right(value)
      case NoSuccess(msg, _) => Left(msg)
    }

  }

  override def evaluate(text: String): Either[String, Float] = {
    parser.toEither(parser.parseAll(parser.expr, text))
  }

}
