package bulkupload

import configdata.MatchingRule
import kits.StrKitLocus
import profile.{AlleleValue, Mitocondrial, MtRCRS, Profile}
import play.api.Logger
import security.User
import types.{AlphanumericId, SampleCode}

import scala.concurrent.Await
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
  errors: Seq[String] = Nil
):
  private val logger = Logger(this.getClass)

  val toAlleleValue = (s: String) => AlleleValue(s.replaceAll(",", "."))

  private def cond[T](p: => Boolean, v: T): Option[T] = if p then Some(v) else None

  def buildWithGenemapperLine(line: List[String]): ProtoProfileBuilder =
    copy(genemapperLine = genemapperLine :+ line)

  def buildWithSampleName(sampleName: String): ProtoProfileBuilder =
    val (err, preex) =
      if this.sampleName == "" then validator.validateSampleName(sampleName)
      else (cond(this.sampleName != sampleName, "error.E0107"), preexistence)
    val errs = err.fold(errors)(errors :+ _)
    copy(sampleName = sampleName, preexistence = preex, errors = errs)

  def buildWithAssigne(assignee: String): ProtoProfileBuilder =
    val err =
      if this.assignee == "" then validator.validateAssignee(assignee)
      else cond(this.assignee != assignee, s"error.E0108")
    val errs = err.fold(errors)(errors :+ _)
    copy(assignee = assignee, errors = errs)

  def buildWithCategory(category: String): ProtoProfileBuilder =
    val (cate, err) =
      if this.category.isRight then
        val c = validator.validateCategory(category).fold[Either[String, String]](Left(category))(x => Right(x.text))
        if this.category.toOption.contains("") then (c, None)
        else (this.category, cond(this.category != c, "error.E0661"))
      else (this.category, None)
    val errs = err.fold(errors)(errors :+ _)
    copy(category = cate, errors = errs)

  def buildWithKit(kit: String): ProtoProfileBuilder =
    val (errOpt, resolvedKit) = validator.validateKit(kit)
    val err =
      if this.kit == "" then errOpt
      else cond(this.kit != resolvedKit, "error.E0690")
    val errs = err.fold(errors)(errors :+ _)
    copy(kit = resolvedKit, errors = errs)

  def buildWithMarker(marker: String, alleles: Seq[String], mitocondrial: Boolean = false): ProtoProfileBuilder =
    val parsed = alleles.filterNot(_.trim.isEmpty).map { v =>
      try Right(toAlleleValue(v))
      catch case e: Throwable =>
        logger.error(s"Error parsing allele value for $marker: ${e.getMessage}")
        Left(e.getMessage)
    }
    val (ok, bad) = parsed.partition(_.isRight)
    val alleleErrors = bad.map(_.left.get)
    val (resVal, mrk) = validator.validateMarker(kit, marker, mitocondrial)
    val errs = alleleErrors.toList ++ resVal.toList ++ errors
    val geno = genotypifications :+ GenotypificationItem(mrk, ok.map(_.toOption.get).toList)
    copy(genotypifications = geno, errors = errs)

  def buildWithErrors(validacion: Option[String]): ProtoProfileBuilder =
    val errs = validacion.fold(errors)(code => errors :+ s"error.$code")
    copy(errors = errs)

  def buildWithAllelesVal(alelos: List[(Mitocondrial, String)], mito: MtRCRS): ProtoProfileBuilder =
    val pos = alelos.collect {
      case (Mitocondrial(base, position), letra) if letra != "borrar" => (base, position.toInt, letra)
    }
    val newErrors = pos.flatMap { case (base, posicion, letra) =>
      mito.tabla.get(posicion) match
        case Some(lO) if lO == letra => Nil
        case None    => List(s"error.E0314 pos $posicion")
        case _       => List(s"error.E0313 pos $posicion")
    }
    copy(errors = errors ++ newErrors)

  def buildWithMtExistente(): ProtoProfileBuilder =
    val existe = validator.validarMtExistente(sampleName)
    if existe && !errors.exists(_.contains("E0315")) then
      copy(errors = errors :+ "error.E0315")
    else this

  def build: ProtoProfile =
    val cat = category.fold(s => """\W""".r.replaceAllIn(s, "_"), identity)
    val cty = category.toOption.map(AlphanumericId(_))
    val buildErrors = preexistence.fold(errors) { gc =>
      val assigneeUser = validator.geneticists.find(_.geneMapperId == assignee)
      assigneeUser.flatMap(u => validator.validateAssigneAndCategory(gc, assignee, cty))
        .fold(errors)(err => err +: errors)
    }
    val stat =
      if buildErrors.isEmpty then
        if category.isLeft then ProtoProfileStatus.Incomplete
        else ProtoProfileStatus.ReadyForApproval
      else ProtoProfileStatus.Invalid
    val gl = genemapperLine.map(_.mkString("\t")).mkString("\n")
    ProtoProfile(id, sampleName, assignee, cat, stat, kit, genotypifications,
      mismatches, matchingRules, buildErrors, gl, preexistence)


case class Validator(
  protoRepo: ProtoProfileRepository,
  kits: Map[String, List[String]],
  kitAlias: Map[String, String],
  locusAlias: Map[String, String],
  geneticists: List[User],
  categoryAlias: Map[String, AlphanumericId]
):
  def validateSampleName(sampleName: String): (Option[String], Option[SampleCode]) =
    val (sampleCodeOpt, batchIdOpt) = Await.result(protoRepo.exists(sampleName), Duration(3, SECONDS))
    val res =
      if sampleCodeOpt.isEmpty && batchIdOpt.isDefined then
        Some(s"error.E0306 sample=$sampleName batch=${batchIdOpt.get}")
      else None
    (res, sampleCodeOpt)

  def validateAssigneAndCategory(globalCode: SampleCode, assigne: String, category: Option[AlphanumericId]): Option[String] =
    Await.result(protoRepo.validateAssigneAndCategory(globalCode, assigne, category), Duration(3, SECONDS))

  def validateKit(kit: String): (Option[String], String) =
    kitAlias.get(kit.toLowerCase).fold((Some(s"error.E0691 kit=$kit"), kit)) { alias =>
      (None, alias)
    }

  def validateMarker(kit: String, marker: String, mitocondrial: Boolean = false): (Seq[String], String) =
    locusAlias.get(marker.toLowerCase).fold {
      val errCode = if mitocondrial then "error.E0310" else "error.E0680"
      (Seq(s"$errCode marker=$marker"), marker)
    } { mrkr =>
      if kits.get(kit.toLowerCase).exists(!_.contains(mrkr)) then
        val errCode = if mitocondrial then "error.E0310" else "error.E0681"
        (Seq(s"$errCode marker=$marker kit=$kit"), marker)
      else (Nil, mrkr)
    }

  def validateAssignee(assignee: String): Option[String] =
    if !geneticists.exists(_.geneMapperId == assignee) then
      Some(s"error.E0650 assignee=$assignee")
    else None

  def validateCategory(category: String): Option[AlphanumericId] =
    categoryAlias.get(category)

  def validarMtExistente(sampleName: String): Boolean =
    Await.result(protoRepo.mtExistente(sampleName), Duration(300, SECONDS))