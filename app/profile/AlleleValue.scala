package profile

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.BSONFormats._
import types._

import java.util.Date
import org.apache.hadoop.record.compiler.JString
import matching.Stringency.Stringency
import configdata.MatchingRule
import matching.MatchingAlgorithm.baseMatch

sealed trait AlleleValue {
  def extendTo(target: AlleleValue): Option[AlleleValue]
}

object AlleleValue {

  val outOfLadderRegEx = """^(\d+)[<|>]$""".r
  val microVariant = """^(\d+)(\.([xX]))?$""".r
  val decimalNumberRegEx = """^(\d+)(\.(\d{1,1}))?$""".r
//  val sequencedAlleleRegEx = """^((\d+)(\.(\d{1,2}))?)(:(\d+))?$""".r
  val xyRegEx = """^([XY])$""".r
  val mtDelDnaRegEx = """^([ACGTBDHRYKMSWNV])(\d+)(DEL)$""".r
  val mtInsDnaRegEx = """^-(\d+.[12])([ACGTBDHRYKMSWNV])$""".r
  val mtSusDnaRegEx = """^([ACGTBDHRYKMSWNV])(\d+)([ACGTBDHRYKMSWNV])$""".r

  //val mtDnaRegExMongo = """^([ACGTURYSWKMBHVN-])@(\d+)$""".r
  val mtDnaRegExMongo = """^([ACGTBDHRYKMSWNV-])@(\d+)$""".r
  val mtInsDnaRegExMongo = """^([ACGTBDHRYKMSWNV])@(\d+.[12])$""".r

  def apply(text: String): AlleleValue = {
    text.replaceAll(",", ".") match {
      case decimalNumberRegEx(_, _, _) => Allele(BigDecimal(text))
      case outOfLadderRegEx(_) => OutOfLadderAllele(BigDecimal(text dropRight 1),text takeRight 1)
      case microVariant(count, _,_) => MicroVariant(Integer.valueOf(count))
//      case sequencedAlleleRegEx(count, _, _, _, _, variation) => SequencedAllele(BigDecimal(count), variation.toInt)
      case xyRegEx(xy) => XY(xy.head)
      case mtDelDnaRegEx(_,_,_) => Mitocondrial('-', BigDecimal(text.substring(1,text.size-3)))
      case mtInsDnaRegEx(base, position) => Mitocondrial(text.charAt(text.size-1), BigDecimal(text.substring(1,text.size-1)))
      case mtSusDnaRegEx(base, position,letra) => Mitocondrial(text.charAt(text.size-1), BigDecimal(text.substring(1,text.size-1)))
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
      case Allele(count)                     => JsNumber(count)
//      case SequencedAllele(count, variation) => JsString(s"$count:$variation")
      case Mitocondrial(base, position)      => JsString(s"${base.toString}@$position")
      case XY(value)                         => JsString(s"$value")
      case MicroVariant(count)          => JsString(s"$count.X")
      case OutOfLadderAllele(count,sign)          => JsString(s"$count$sign")
    }
  }
}


case class Allele(count: BigDecimal) extends AlleleValue {
  def extendTo(target: AlleleValue): Option[AlleleValue] = target match {
    case allele: Allele => {
      if (allele == this)
        Some(allele)
      else
        None
    }
//    case allele: SequencedAllele => {
//      if (allele.count == this.count)
//        Some(allele)
//      else
//        None
//    }
    case allele: MicroVariant => None
    case alelle: OutOfLadderAllele => None
    case allele: Mitocondrial    => None
    case allele: XY              => None
  }
}

//case class SequencedAllele(count: BigDecimal, variation: Int) extends AlleleValue {
//  def extendTo(target: AlleleValue): Option[AlleleValue] = if (this == target) Some(target) else None
//}

case class Mitocondrial(base: Char, position: BigDecimal) extends AlleleValue {
  def extendTo(allele: AlleleValue): Option[AlleleValue] = {
    if (allele == this) {
      Option(allele)
    } else {
      None
    }
  }
  def hasSamePosition(other:Mitocondrial): Boolean = {
    this.position.equals(other.position)
  }
  /**
   * Checks that a base mutation matches ambiguosly with other base, and that
   * the position of the mutation is the same.
   */
  def isExactMatch(other: Mitocondrial): Boolean = {
    this.hasSamePosition(other) && baseMatch(this.base, other.base)
  }
  def mismatchInSamePosition(other:Mitocondrial): Boolean = {
    this.hasSamePosition(other) && !baseMatch(this.base, other.base)
  }
  
  def before(other:Mitocondrial): Boolean = {
    this.position < other.position
  }
  
  def after(other: Mitocondrial): Boolean = {
    this.position > other.position
  }
  
  def matchReference(mtRCRS: MtRCRS): Boolean = {
    val pos:Option[Int] = if (this.position % 1 == 0) {
      Option(this.position.toInt)
    } else {
      None
    }
    val compareThisBaseToReference = (refBase:String) => {
      refBase
        .length
        .equals(1)
        .&&(
          baseMatch(
            refBase.toCharArray.head,
            this.base
          )
        )
    }
    val compareToRefPosition = (p:Int) => {
      mtRCRS
        .tabla
        .get(p)
        .fold(false)(compareThisBaseToReference)
    }
    pos.fold(false)(compareToRefPosition)
  }
}

case class XY(value: Char) extends AlleleValue {
  def extendTo(target: AlleleValue): Option[AlleleValue] = if (this == target) Some(target) else None
}

case class OutOfLadderAllele(base: BigDecimal, sign: String) extends AlleleValue {
  def extendTo(target: AlleleValue): Option[AlleleValue] = target match {
    case alelle: Allele => None
    case alelle: MicroVariant => None
    case alelle: OutOfLadderAllele => {
      if (alelle.base == this.base && alelle.sign == this.sign)
        Some(alelle)
      else
        None
    }
    case alelle: Mitocondrial    => None
    case alelle: XY              => None
  }
  override def equals(o: scala.Any): Boolean = false
}

case class MicroVariant(count: Integer) extends AlleleValue {
  def extendTo(target: AlleleValue): Option[AlleleValue] = target match {
    case alelle: Allele => None
    case alelle: MicroVariant => {
      if (alelle.count == this.count)
        Some(alelle)
      else
        None
    }
    case alelle: OutOfLadderAllele => None
    case alelle: Mitocondrial    => None
    case alelle: XY              => None
  }

  override def equals(o: scala.Any): Boolean = false
}