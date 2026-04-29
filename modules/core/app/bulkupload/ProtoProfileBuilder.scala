package bulkupload

import configdata.MatchingRule
import profile.{AlleleValue, Mitocondrial, MtRCRS, Profile}
import play.api.Logger
import play.api.i18n.Messages
import types.{AlphanumericId, SampleCode}
import user.UserView

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, SECONDS}

case class ProtoProfileBuilder(
    validator: Validator,
    id: Long = 0,
    sampleName: String = "",
    assignee: String = "",
    category: Either[String, String] = Right(""),
    kit: String = "",
    genotypifications: ProtoProfile.Genotypification = List(),
    mismatches: Profile.Mismatch = Map.empty,
    matchingRules: Seq[MatchingRule] = Nil,
    preexistence: Option[SampleCode] = None,
    genemapperLine: Seq[Seq[String]] = Nil,
    errors: Seq[String] = Nil) {

  def this(validator: Validator) = this(validator, 0)

  private val logger = Logger(this.getClass())

  private val toAlleleValue = (s: String) => AlleleValue(s.replaceAll(",", "."))

  private def cond[T](p: => Boolean, v: T): Option[T] = if (p) Some(v) else None

  private def msg(key: String, args: Any*): String = validator.messages(key, args*)

  def buildWithGenemapperLine(line: List[String]): ProtoProfileBuilder =
    ProtoProfileBuilder(this.validator, this.id, this.sampleName, this.assignee, this.category, this.kit, this.genotypifications, this.mismatches, this.matchingRules, this.preexistence, this.genemapperLine :+ line, this.errors)

  def buildWithSampleName(sampleName: String): ProtoProfileBuilder = {
    val (err, preex) = if (this.sampleName == "")
      validator.validateSampleName(sampleName)
    else
      (cond(this.sampleName != sampleName, msg("error.E0107")), this.preexistence)

    val errors = err.fold(this.errors)(error => this.errors :+ error)
    ProtoProfileBuilder(this.validator, this.id, sampleName, this.assignee, this.category, this.kit, this.genotypifications, this.mismatches, this.matchingRules, preex, this.genemapperLine, errors)
  }

  def buildWithAssigne(assignee: String): ProtoProfileBuilder = {
    val err = if (this.assignee == "")
      validator.validateAssignee(assignee)
    else
      cond(this.assignee != assignee, msg("error.E0108", this.sampleName))

    val errors = err.fold(this.errors)(error => this.errors :+ error)
    ProtoProfileBuilder(this.validator, this.id, this.sampleName, assignee, this.category, this.kit, this.genotypifications, this.mismatches, this.matchingRules, this.preexistence, this.genemapperLine, errors)
  }

  def buildWithCategory(category: String): ProtoProfileBuilder = {
    val (cate, err) = if (this.category.isRight) {
      val c = validator.validateCategory(category).fold[Either[String, String]](Left(category))(x => Right(x.text))
      if (this.category.toOption.get == "") {
        (c, None)
      } else {
        val err = cond(this.category != c, msg("error.E0661", this.sampleName))
        (this.category, err)
      }
    } else {
      (this.category, None)
    }

    val errors = err.fold(this.errors)(error => this.errors :+ error)
    ProtoProfileBuilder(this.validator, this.id, this.sampleName, this.assignee, cate, this.kit, this.genotypifications, this.mismatches, this.matchingRules, this.preexistence, this.genemapperLine, errors)
  }

  def buildWithKit(kit: String): ProtoProfileBuilder = {
    val ee = validator.validateKit(kit)

    val err = if (this.kit == "")
      ee._1
    else
      cond(this.kit != ee._2, msg("error.E0690", this.sampleName))

    val errors = err.fold(this.errors)(error => this.errors :+ error)
    ProtoProfileBuilder(this.validator, this.id, this.sampleName, this.assignee, this.category, ee._2, this.genotypifications, this.mismatches, this.matchingRules, this.preexistence, this.genemapperLine, errors)
  }

  def buildWithMarker(
    marker: String,
    alleles: Seq[String],
    mitocondrial: Boolean = false
  ): ProtoProfileBuilder = {
    val allelesVal = alleles
      .filterNot(_.trim.isEmpty)
      .map { alleleVal =>
        try Right(toAlleleValue(alleleVal))
        catch {
          case e: Throwable =>
            logger.error(s"Error parsing allele value for $marker: ${e.getMessage}")
            Left(e.getMessage)
        }
      }
    val (alleleErrors, ok) = allelesVal.partitionMap(identity)
    val (resVal, mrk) = validator.validateMarker(this.kit, marker, mitocondrial)

    val errors = cond(alleleErrors.nonEmpty, alleleErrors).getOrElse(Nil) ++ resVal ++ this.errors

    val geno = this.genotypifications :+ GenotypificationItem(mrk, ok.toList)
    ProtoProfileBuilder(this.validator, this.id, this.sampleName, this.assignee, this.category, this.kit, geno, this.mismatches, this.matchingRules, this.preexistence, this.genemapperLine, errors)
  }

  private val normalizeRe = """\W""".r

  def build: ProtoProfile = {
    val cat = category.fold(
      geneMapperCategory => normalizeRe.replaceAllIn(geneMapperCategory, "_"),
      genisCat => genisCat
    )
    val cty = category.fold[Option[AlphanumericId]](_ => None, fb => Some(AlphanumericId(fb)))
    val buildErrors = this.preexistence.fold(this.errors)(gc =>
      validator.validateAssigneAndCategory(gc, this.validator.geneticists.find(_.geneMapperId == this.assignee).get.userName, cty).fold(this.errors)(err =>
        this.errors.+:(err)))

    val stat =
      if (buildErrors.isEmpty) {
        if (category.isLeft) ProtoProfileStatus.Incomplete
        else ProtoProfileStatus.ReadyForApproval
      } else {
        ProtoProfileStatus.Invalid
      }

    val gl = this.genemapperLine.map(_.mkString("\t")).mkString("\n")

    ProtoProfile(this.id, this.sampleName, this.assignee, cat, stat, this.kit, this.genotypifications, this.mismatches, this.matchingRules, buildErrors, gl, this.preexistence)
  }

  def buildWithErrors(validacion: Option[String]): ProtoProfileBuilder = {
    val err = validacion.map(x => msg(s"error.$x"))
    val errors = err.fold(this.errors)(error => this.errors :+ error)
    ProtoProfileBuilder(this.validator, this.id, this.sampleName, this.assignee, this.category, this.kit, this.genotypifications, this.mismatches, this.matchingRules, this.preexistence, this.genemapperLine, errors)
  }

  def buildWithAllelesVal(
    alelos: List[(Mitocondrial, String)],
    mito: MtRCRS
  ): ProtoProfileBuilder = {
    var errores = this.errors
    val pos = alelos
      .map {
        case (Mitocondrial(base, position), letra) => base -> position.toInt -> letra
        case _ => 'b' -> 0 -> "borrar"
      }
      .filter(_._2 != "borrar")
    if (pos.nonEmpty) {
      val validacion = pos.map { posicion =>
        val letraOriginal = mito.tabla.get(posicion._1._2)
        letraOriginal match {
          case Some(lO) if lO == posicion._2 => (0, 0)
          case None => (14, posicion._1._2)
          case _ => (13, posicion._1._2)
        }
      }

      validacion.foreach { error =>
        val err = cond(error._1 != 0, msg("error.E03" + error._1, error._2))
        errores = err.fold(errores)(e => errores :+ e)
      }
    }

    ProtoProfileBuilder(this.validator, this.id, this.sampleName, this.assignee, this.category, this.kit, this.genotypifications, this.mismatches, this.matchingRules, this.preexistence, this.genemapperLine, errores)
  }

  def buildWithMtExistente(): ProtoProfileBuilder = {
    var errores = this.errors
    val existe = validator.validarMtExistente(this.sampleName)
    if (existe) {
      val msgKey = if (errores.nonEmpty) {
        val existe = errores.exists { x =>
          val exist = false
          x.contains("E0315")
          !exist
        }
        if (!existe) "error.E0315" else "borrar"
      } else {
        "error.E0315"
      }
      val err = cond(msgKey != "borrar", msg(msgKey))
      errores = err.fold(errores)(e => errores :+ e)
    }

    ProtoProfileBuilder(this.validator, this.id, this.sampleName, this.assignee, this.category, this.kit, this.genotypifications, this.mismatches, this.matchingRules, this.preexistence, this.genemapperLine, errores)
  }
}

case class Validator(
    protoRepo: ProtoProfileRepository,
    kits: Map[String, List[String]],
    kitAlias: Map[String, String],
    locusAlias: Map[String, String],
    geneticists: List[UserView],
    categoryAlias: Map[String, AlphanumericId],
    messages: Messages) {

  def validateSampleName(sampleName: String): (Option[String], Option[SampleCode]) = {
    val (sampleCodeOpt, batchIdOpt): (Option[SampleCode], Option[Long]) = Await.result(protoRepo.exists(sampleName), Duration(3, SECONDS))
    val res = if (sampleCodeOpt.isEmpty && batchIdOpt.isDefined)
      Some(messages("error.E0306", sampleName, batchIdOpt.get))
    else None
    (res, sampleCodeOpt)
  }

  def validateAssigneAndCategory(globalCode: SampleCode, assigne: String, category: Option[AlphanumericId]): Option[String] =
    Await.result(protoRepo.validateAssigneAndCategory(globalCode, assigne, category), Duration(3, SECONDS))

  def validateKit(kit: String): (Option[String], String) =
    kitAlias.get(kit.toLowerCase).fold((Option(messages("error.E0691", kit)), kit)) { alias =>
      (None, alias)
    }

  def validateMarker(kit: String, marker: String, mitocondrial: Boolean = false): (Seq[String], String) =
    locusAlias.get(marker.toLowerCase).fold((Seq(messages(if (mitocondrial) "error.E0310" else "error.E0680", marker)), marker)) { mrkr =>
      if (kits.get(kit.toLowerCase).map(!_.contains(mrkr)).getOrElse(false))
        (Seq(messages(if (mitocondrial) "error.E0310" else "error.E0681", marker, kit)), marker)
      else
        (Nil, mrkr)
    }

  def validateAssignee(assignee: String): Option[String] =
    if (!geneticists.exists(_.geneMapperId == assignee))
      Some(messages("error.E0650", assignee))
    else
      None

  def validateCategory(category: String): Option[AlphanumericId] =
    categoryAlias.get(category)

  def validarMtExistente(sampleName: String): Boolean =
    Await.result(protoRepo.mtExistente(sampleName), Duration(300, SECONDS))
}
