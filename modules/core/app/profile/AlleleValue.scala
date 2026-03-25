package profile

import play.api.libs.json.*
import play.api.libs.functional.syntax.*
import matching.MtDnaUtils.baseMatch

sealed trait AlleleValue {
  def extendTo(target: AlleleValue): Option[AlleleValue]
}

object AlleleValue {
  val outOfLadderRegEx = """^(\d+)[<|>]$""".r
  val microVariant = """^(\d+)(\.([xX]))?$""".r
  val decimalNumberRegEx = """^(\d+)(\.(\d{1,1}))?$""".r
  val xyRegEx = """^([XY])$""".r
  val mtDelDnaRegEx = """^([ACGTBDHRYKMSWNV])(\d+)(DEL)$""".r
  val mtInsDnaRegEx = """^-*(\d+.[12])([ACGTBDHRYKMSWNV])$""".r
  val mtSusDnaRegEx = """^([ACGTBDHRYKMSWNV])(\d+)([ACGTBDHRYKMSWNV])$""".r
  val mtDnaRegExMongo = """^([ACGTBDHRYKMSWNV-])@(\d+)$""".r
  val mtInsDnaRegExMongo = """^([ACGTBDHRYKMSWNV])@(\d+.[12])$""".r

  def apply(text: String): AlleleValue = {
    text.replaceAll(",", ".") match {
      case decimalNumberRegEx(_, _, _) => Allele(BigDecimal(text))
      case outOfLadderRegEx(_) => OutOfLadderAllele(BigDecimal(text.dropRight(1)), text.takeRight(1))
      case microVariant(count, _, _) => MicroVariant(Integer.valueOf(count))
      case xyRegEx(xy) => XY(xy.head)
      case mtDelDnaRegEx(_, _, _) => Mitocondrial('-', BigDecimal(text.substring(1, text.length - 3)))
      case mtInsDnaRegEx(position, base) => Mitocondrial(base.head, BigDecimal(position))
      case mtSusDnaRegEx(base, position, letra) => Mitocondrial(text.charAt(text.length - 1), BigDecimal(text.substring(1, text.length - 1)))
      case mtDnaRegExMongo(base, position) => Mitocondrial(base.head, BigDecimal(position))
      case mtInsDnaRegExMongo(base, position) => Mitocondrial(base.head, BigDecimal(position))
      case _ => throw new IllegalArgumentException(s"Allele Value Expected, but found '$text'")
    }
  }

  implicit val alleleReads: Reads[AlleleValue] = new Reads[AlleleValue] {
    def reads(json: JsValue): JsResult[AlleleValue] = {
      try {
        val alleleTxt = json match {
          case JsString(allele) => allele
          case JsNumber(allele) => allele.toString
          case json             => json.toString
        }
        JsSuccess(AlleleValue(alleleTxt))
      } catch {
        case err: IllegalArgumentException => JsError(err.getMessage)
      }
    }
  }

  implicit val alleleWrites: Writes[AlleleValue] = new Writes[AlleleValue] {
    def writes(alleleValue: AlleleValue): JsValue = alleleValue match {
      case Allele(count)                => JsNumber(count)
      case Mitocondrial(base, position) => JsString(s"${base.toString}@$position")
      case XY(value)                    => JsString(s"$value")
      case MicroVariant(count)          => JsString(s"$count.X")
      case OutOfLadderAllele(count, sign) => JsString(s"$count$sign")
    }
  }
}

case class Allele(count: BigDecimal) extends AlleleValue {
  def extendTo(target: AlleleValue): Option[AlleleValue] = target match {
    case allele: Allele =>
      if (allele == this) Some(allele) else None
    case _: MicroVariant      => None
    case _: OutOfLadderAllele => None
    case _: Mitocondrial      => None
    case _: XY                => None
  }
}

case class Mitocondrial(base: Char, position: BigDecimal) extends AlleleValue {
  def extendTo(allele: AlleleValue): Option[AlleleValue] = {
    if (allele == this) Option(allele) else None
  }

  def hasSamePosition(other: Mitocondrial): Boolean =
    this.position.equals(other.position)

  def isExactMatch(other: Mitocondrial): Boolean =
    this.hasSamePosition(other) && baseMatch(this.base, other.base)

  def mismatchInSamePosition(other: Mitocondrial): Boolean =
    this.hasSamePosition(other) && !baseMatch(this.base, other.base)

  def before(other: Mitocondrial): Boolean =
    this.position < other.position

  def after(other: Mitocondrial): Boolean =
    this.position > other.position

  def matchReference(mtRCRS: MtRCRS): Boolean = {
    val pos: Option[Int] = if (this.position % 1 == 0) {
      Option(this.position.toInt)
    } else {
      None
    }

    val compareThisBaseToReference = (refBase: String) => {
      refBase.length.equals(1) && baseMatch(refBase.toCharArray.head, this.base)
    }

    val compareToRefPosition = (p: Int) => {
      mtRCRS.tabla.get(p).fold(false)(compareThisBaseToReference)
    }

    pos.fold(false)(compareToRefPosition)
  }
}

case class XY(value: Char) extends AlleleValue {
  def extendTo(target: AlleleValue): Option[AlleleValue] =
    if (this == target) Some(target) else None
}

case class OutOfLadderAllele(base: BigDecimal, sign: String) extends AlleleValue {
  def extendTo(target: AlleleValue): Option[AlleleValue] = target match {
    case _: Allele            => None
    case _: MicroVariant      => None
    case other: OutOfLadderAllele =>
      if (other.base == this.base && other.sign == this.sign) Some(other) else None
    case _: Mitocondrial => None
    case _: XY           => None
  }

  override def equals(o: scala.Any): Boolean = false
}

case class MicroVariant(count: Integer) extends AlleleValue {
  def extendTo(target: AlleleValue): Option[AlleleValue] = target match {
    case _: Allele => None
    case other: MicroVariant =>
      if (other.count == this.count) Some(other) else None
    case _: OutOfLadderAllele => None
    case _: Mitocondrial      => None
    case _: XY                => None
  }

  override def equals(o: scala.Any): Boolean = false
}
