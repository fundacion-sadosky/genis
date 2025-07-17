package trace

import configdata.{CategoryAssociation, CategoryConfiguration}
import play.api.libs.json.{Json, Writes, _}
import profile.Profile
import types.SampleCode

trait TraceInfo {
  val description: String
  val kind: TraceType.Value
}

case class AnalysisInfo(
                         loci: Seq[Profile.Marker],
                         kit: Option[String],
                         analysisType: Option[Int],
                         categoryConfiguration: CategoryConfiguration) extends TraceInfo {
  override val kind = TraceType.analysis
  override val description =
    if (kit.isDefined) {
      s"Alta de análisis del " +
        s"Kit ${kit.get} con Marcadores ${loci.mkString(", ")}."
    } else {
      s"Alta de análisis con Marcadores ${loci.mkString(", ")}."
    }
}

case class AssociationInfo(
                            profile: SampleCode,
                            user: String,
                            categoryAssociations: Seq[CategoryAssociation]) extends TraceInfo {
  override val kind = TraceType.association
  override val description =
    s"Asociación con el perfil ${profile.text} cuyo responsable es $user."
}

case object ProfileDataInfo extends TraceInfo {
  override val kind = TraceType.profileData
  override val description = s"Carga de la metadata."
}

case class ProfileCategoryModificationInfo(
                                            oldCategory: String,
                                            newCategory: String
                                          ) extends TraceInfo {
  override val kind = TraceType.categoryModification
  override val description =
    s"Modificación de la categoría $oldCategory a $newCategory."
}

case class SuperiorInstanceCategoryModificationInfo(
                                                     oldCategory: String,
                                                     newCategory: String
                                                   ) extends TraceInfo {
  override val kind = TraceType.superiorInstanceCategoryModification
  override val description = s"Se aceptó el cambio de categoría de la" +
    s" instancia inferior." +
    s"($oldCategory a $newCategory)."
}

case object SuperiorCategoryChangeRejectedInfo extends TraceInfo {
  override val kind = TraceType.superiorInstanceCategoryRejection
  override val description = s"Se rechazó un cambio de categoría proveniente " +
    s"de una instancia inferior."
}

case object ProfileAprovedInSuperiorInfo extends TraceInfo {
  override val kind = TraceType.interconectionAproved
  override val description = s"Aprobado en instancia superior."
}

case object ProfileCategoryChangeAprovedInSuperiorInfo extends TraceInfo {
  override val kind = TraceType.interconectionCategoryAproved
  override val description = s"El cambio de categoría fue aceptado en " +
    s"instancia superior."
}

case object ProfileImportedFromInferiorInfo extends TraceInfo {
  override val kind = TraceType.importedFromInferior
  override val description = s"Importado desde una instancia inferior."
}

case class ProfileRejectedInSuperiorInfo(motive: String) extends TraceInfo {
  override val kind = TraceType.interconectionRejected
  override val description = s"Rechazado en instancia superior. Motivo: $motive."
}
// 2 Nuevos
case class InterconnectionDeletedInInferiorInfo(motive: String) extends TraceInfo {
  override val kind = TraceType.interconnectionDeletedInInferior
  val motiveParts = motive.split(",").map(_.trim)
  val solicitor = if (motiveParts.nonEmpty) motiveParts(0) else ""
  val motiveText = if (motiveParts.length > 1) motiveParts(1) else ""
  override val description = s"Perfil eliminado en la instancia inferior. Baja solicitada por: $solicitor. Motivo: $motiveText."
}

case class InterconnectionDeletedInSuperiorInfo(motive: String) extends TraceInfo {
  override val kind = TraceType.interconnectionDeletedInSuperior
  val motiveParts = motive.split(",").map(_.trim)
  val solicitor = if (motiveParts.nonEmpty) motiveParts(0) else ""
  val motiveText = if (motiveParts.length > 1) motiveParts(1) else ""
  override val description = s"Perfil eliminado en la instancia superior. Baja solicitada por: $solicitor. Motivo: $motiveText."
}

case object CategoryChangeRejectedInSupInfo extends TraceInfo {
  override val kind = TraceType.interconectionCategoryRejected
  override val description =
    s"El Cambio de categoría fue rechazado en instancia superior."
}

case object ProfileInterconectionUploadInfo extends TraceInfo {
  override val kind = TraceType.interconectionUpdload
  override val description = s"Replicado a instancia superior."
}

case class DeleteInfo(
                       solicitor: String,
                       motive: String) extends TraceInfo {
  override val kind = TraceType.delete
  override val description =
    s"Baja del perfil solicitada por $solicitor con motivo: $motive."
}

case class PedigreeStatusChangeInfo(status:String) extends TraceInfo{
  override val kind = TraceType.pedigreeStatusChange
  def getStatus(s1:String):String = {
    s1 match {
      case "UnderConstruction" => "En construcción"
      case "Active"=> "Activo"
      case "Validated"=> "Confirmado"
      case "Deleted"=> "Borrado"
      case "Closed"=> "Cerrado"
      case "Open"=> "Abierto"
      case _ => s1
    }
  }
  val stat = getStatus(status)
  override val description = s"Cambio al estado $stat."
}
case class PedigreeCopyInfo(pedigreeId:Long,name:String) extends TraceInfo{
  override val kind = TraceType.pedigriCopy
  override val description = s"Copia del pedigrí con el nombre $name."
}
case class PedigreeEditInfo(pedigreeId:Long) extends TraceInfo{
  override val kind = TraceType.pedigriEdit
  override val description = s"Modificación del pedigrí."
}
case class PedigreeNewScenarioInfo(id:String,nombre:String) extends TraceInfo{
  override val kind = TraceType.pedigriNewScenario
  override val description =
    s"Creación de un nuevo escenario con nombre $nombre."
}
object TraceInfo {

  implicit val analysisFormat = Json.format[AnalysisInfo]
  implicit val matchProcessFormat = Json.format[MatchProcessInfo]
  implicit val matchFormat = Json.format[MatchInfo]
  implicit val hitFormat = Json.format[HitInfo]
  implicit val discardFormat = Json.format[DiscardInfo]
  implicit val associationFormat = Json.format[AssociationInfo]
  implicit val profileDataFormat = Format(
    new Reads[ProfileDataInfo.type] {
      def reads(js: JsValue): JsResult[ProfileDataInfo.type] =
        JsSuccess(ProfileDataInfo)
    },
    new Writes[ProfileDataInfo.type] {
      def writes(ti: ProfileDataInfo.type): JsValue = Json.obj()
    })
  implicit val profileInterconectionUploadFormat = Format(
    new Reads[ProfileInterconectionUploadInfo.type] {
      def reads(js: JsValue): JsResult[ProfileInterconectionUploadInfo.type] =
        JsSuccess(ProfileInterconectionUploadInfo)
    },
    new Writes[ProfileInterconectionUploadInfo.type] {
      def writes(ti: ProfileInterconectionUploadInfo.type): JsValue = Json.obj()
    })
  implicit val profileAprovedInSuperiorFormat = Format(
    new Reads[ProfileAprovedInSuperiorInfo.type] {
      def reads(js: JsValue): JsResult[ProfileAprovedInSuperiorInfo.type] =
        JsSuccess(ProfileAprovedInSuperiorInfo)
    },
    new Writes[ProfileAprovedInSuperiorInfo.type] {
      def writes(ti: ProfileAprovedInSuperiorInfo.type): JsValue = Json.obj()
    })

  implicit val categoryAprovedInSuperiorFormat = Format(
    new Reads[ProfileCategoryChangeAprovedInSuperiorInfo.type] {
      def reads(js: JsValue)
      :JsResult[ProfileCategoryChangeAprovedInSuperiorInfo.type]
      = JsSuccess(ProfileCategoryChangeAprovedInSuperiorInfo)
    },
    new Writes[ProfileCategoryChangeAprovedInSuperiorInfo.type] {
      def writes(ti: ProfileCategoryChangeAprovedInSuperiorInfo.type)
      :JsValue = Json.obj()
    }
  )
  implicit val profileImportedFromInferiorFormat = Format(
    new Reads[ProfileImportedFromInferiorInfo.type] {
      def reads(js: JsValue): JsResult[ProfileImportedFromInferiorInfo.type] =
        JsSuccess(ProfileImportedFromInferiorInfo)
    },
    new Writes[ProfileImportedFromInferiorInfo.type] {
      def writes(ti: ProfileImportedFromInferiorInfo.type): JsValue = Json.obj()
    }
  )
  implicit val profileRejectedInSuperiorFormat: Format[ProfileRejectedInSuperiorInfo] = Json.format[ProfileRejectedInSuperiorInfo]
  // 2 Nuevos
  implicit val interconnectionDeletedInInferiorFormat: Format[InterconnectionDeletedInInferiorInfo] = Json.format[InterconnectionDeletedInInferiorInfo]
  implicit val interconnectionDeletedInSuperiorFormat: Format[InterconnectionDeletedInSuperiorInfo] = Json.format[InterconnectionDeletedInSuperiorInfo]

  implicit val catModRejectedInSupFormat = Format(
    new Reads[CategoryChangeRejectedInSupInfo.type] {
      def reads(js: JsValue): JsResult[CategoryChangeRejectedInSupInfo.type] =
        JsSuccess(CategoryChangeRejectedInSupInfo)
    },
    new Writes[CategoryChangeRejectedInSupInfo.type] {
      def writes(ti: CategoryChangeRejectedInSupInfo.type): JsValue = Json.obj()
    }
  )

  implicit val superiorInstCatRejectFormat = Format(
    new Reads[SuperiorCategoryChangeRejectedInfo.type] {
      def reads(js: JsValue): JsResult[SuperiorCategoryChangeRejectedInfo.type] =
        JsSuccess(SuperiorCategoryChangeRejectedInfo)
    },
    new Writes[SuperiorCategoryChangeRejectedInfo.type] {
      def writes(ti: SuperiorCategoryChangeRejectedInfo.type): JsValue =
        Json.obj()
    })

  implicit val deleteFormat = Json.format[DeleteInfo]
  implicit val pedigreeMatchProcessFormat = Json.format[PedigreeMatchProcessInfo]
  implicit val pedigreeMatchFormat = Json.format[PedigreeMatchInfo]
  implicit val pedigreeDiscardFormat = Json.format[PedigreeDiscardInfo]
  implicit val pedigreeConfirmFormat = Json.format[PedigreeConfirmInfo]
  implicit val pedigreeStatusChangeFormat = Json.format[PedigreeStatusChangeInfo]
  implicit val pedigreeCopyFormat = Json.format[PedigreeCopyInfo]
  implicit val pedigreeEditFormat = Json.format[PedigreeEditInfo]
  implicit val pedigreeNewScenarioFormat = Json.format[PedigreeNewScenarioInfo]
  implicit val pedigreeMatchFormat2 = Json.format[PedigreeMatchInfo2]
  implicit val pedigreeDiscardFormat2 = Json.format[PedigreeDiscardInfo2]
  implicit val pedigreeConfirmFormat2 = Json.format[PedigreeConfirmInfo2]
  implicit val profileCategoryModificationFormat = Json
    .format[ProfileCategoryModificationInfo]
  implicit val superiorInstCatModFormat = Json
    .format[SuperiorInstanceCategoryModificationInfo]

  def unapply(info: TraceInfo): Option[(TraceType.Value, JsValue)] = {
    info match {
      case x: AnalysisInfo => Some((x.kind, Json.toJson(x)(analysisFormat)))
      case x: MatchProcessInfo =>
        Some((x.kind, Json.toJson(x)(matchProcessFormat)))
      case x: MatchInfo => Some((x.kind, Json.toJson(x)(matchFormat)))
      case x: HitInfo => Some((x.kind, Json.toJson(x)(hitFormat)))
      case x: DiscardInfo => Some((x.kind, Json.toJson(x)(discardFormat)))
      case x: AssociationInfo =>
        Some((x.kind, Json.toJson(x)(associationFormat)))
      case x: ProfileDataInfo.type =>
        Some((x.kind, Json.toJson(x)(profileDataFormat)))
      case x: ProfileInterconectionUploadInfo.type =>
        Some((x.kind, Json.toJson(x)(profileInterconectionUploadFormat)))
      case x: DeleteInfo => Some((x.kind, Json.toJson(x)(deleteFormat)))
      case x: PedigreeMatchProcessInfo =>
        Some((x.kind, Json.toJson(x)(pedigreeMatchProcessFormat)))
      case x: PedigreeMatchInfo =>
        Some((x.kind, Json.toJson(x)(pedigreeMatchFormat)))
      case x: PedigreeDiscardInfo =>
        Some((x.kind, Json.toJson(x)(pedigreeDiscardFormat)))
      case x: PedigreeConfirmInfo =>
        Some((x.kind, Json.toJson(x)(pedigreeConfirmFormat)))
      case x: PedigreeStatusChangeInfo =>
        Some((x.kind, Json.toJson(x)(pedigreeStatusChangeFormat)))
      case x: PedigreeCopyInfo =>
        Some((x.kind, Json.toJson(x)(pedigreeCopyFormat)))
      case x: PedigreeEditInfo =>
        Some((x.kind, Json.toJson(x)(pedigreeEditFormat)))
      case x: PedigreeNewScenarioInfo =>
        Some((x.kind, Json.toJson(x)(pedigreeNewScenarioFormat)))
      case x: PedigreeMatchInfo2 =>
        Some((x.kind, Json.toJson(x)(pedigreeMatchFormat2)))
      case x: PedigreeDiscardInfo2 =>
        Some((x.kind, Json.toJson(x)(pedigreeDiscardFormat2)))
      case x: PedigreeConfirmInfo2 =>
        Some((x.kind, Json.toJson(x)(pedigreeConfirmFormat2)))
      case x: ProfileAprovedInSuperiorInfo.type =>
        Some((x.kind, Json.toJson(x)(profileAprovedInSuperiorFormat)))
      case x: ProfileCategoryChangeAprovedInSuperiorInfo.type =>
        Some((x.kind, Json.toJson(x)(categoryAprovedInSuperiorFormat)))
      case x: ProfileImportedFromInferiorInfo.type =>
        Some((x.kind, Json.toJson(x)(profileImportedFromInferiorFormat)))
      case x: ProfileRejectedInSuperiorInfo =>
        Some((x.kind, Json.toJson(x)(profileRejectedInSuperiorFormat)))
      case x: CategoryChangeRejectedInSupInfo.type =>
        Some((x.kind, Json.toJson(x)(catModRejectedInSupFormat)))
      case x: ProfileCategoryModificationInfo =>
        Some((x.kind, Json.toJson(x)(profileCategoryModificationFormat)))
      case x: SuperiorInstanceCategoryModificationInfo =>
        Some((x.kind, Json.toJson(x)(superiorInstCatModFormat)))
      case x: SuperiorCategoryChangeRejectedInfo.type =>
        Some((x.kind, Json.toJson(x)(superiorInstCatRejectFormat)))
      case x: InterconnectionDeletedInInferiorInfo =>
        Some((x.kind, Json.toJson(x)(interconnectionDeletedInInferiorFormat)))
      case x: InterconnectionDeletedInSuperiorInfo =>
        Some((x.kind, Json.toJson(x)(interconnectionDeletedInSuperiorFormat)))
      case _ => None
    }
  }

  def apply(kind: TraceType.Value, json: JsValue): TraceInfo = {
    (
      kind match {
        case TraceType.analysis => Json.fromJson[AnalysisInfo](json)
        case TraceType.matchProcess => Json.fromJson[MatchProcessInfo](json)
        case TraceType.`match` => Json.fromJson[MatchInfo](json)
        case TraceType.hit => Json.fromJson[HitInfo](json)
        case TraceType.discard => Json.fromJson[DiscardInfo](json)
        case TraceType.association => Json.fromJson[AssociationInfo](json)
        case TraceType.profileData => Json.fromJson[ProfileDataInfo.type](json)
        case TraceType.interconectionUpdload =>
          Json.fromJson[ProfileInterconectionUploadInfo.type](json)
        case TraceType.delete => Json.fromJson[DeleteInfo](json)
        case TraceType.pedigreeMatchProcess =>
          Json.fromJson[PedigreeMatchProcessInfo](json)
        case TraceType.pedigreeMatch => Json.fromJson[PedigreeMatchInfo](json)
        case TraceType.pedigreeDiscard =>
          Json.fromJson[PedigreeDiscardInfo](json)
        case TraceType.pedigreeConfirm =>
          Json.fromJson[PedigreeConfirmInfo](json)
        case TraceType.pedigreeStatusChange =>
          Json.fromJson[PedigreeStatusChangeInfo](json)
        case TraceType.pedigriCopy => Json.fromJson[PedigreeCopyInfo](json)
        case TraceType.pedigriEdit => Json.fromJson[PedigreeEditInfo](json)
        case TraceType.pedigriNewScenario =>
          Json.fromJson[PedigreeNewScenarioInfo](json)
        case TraceType.pedigreeMatch2 => Json.fromJson[PedigreeMatchInfo2](json)
        case TraceType.pedigreeDiscard2 =>
          Json.fromJson[PedigreeDiscardInfo2](json)
        case TraceType.pedigreeConfirm2 =>
          Json.fromJson[PedigreeConfirmInfo2](json)
        case TraceType.interconectionAproved =>
          Json.fromJson[ProfileAprovedInSuperiorInfo.type](json)
        case TraceType.interconectionCategoryAproved =>
          Json.fromJson[ProfileCategoryChangeAprovedInSuperiorInfo.type](json)
        case TraceType.importedFromInferior =>
          Json.fromJson[ProfileImportedFromInferiorInfo.type](json)
        case TraceType.interconectionRejected =>
          Json.fromJson[ProfileRejectedInSuperiorInfo](json)(profileRejectedInSuperiorFormat)
        case TraceType.interconectionCategoryRejected => Json
          .fromJson[CategoryChangeRejectedInSupInfo.type](json)
        case TraceType.categoryModification => Json
          .fromJson[ProfileCategoryModificationInfo](json)
        case TraceType.superiorInstanceCategoryModification => Json
          .fromJson[SuperiorInstanceCategoryModificationInfo](json)
        case TraceType.superiorInstanceCategoryRejection => Json
          .fromJson[SuperiorCategoryChangeRejectedInfo.type](json)
        case TraceType.interconnectionDeletedInInferior =>
          Json.fromJson[InterconnectionDeletedInInferiorInfo](json)(interconnectionDeletedInInferiorFormat)
        case TraceType.interconnectionDeletedInSuperior =>
          Json.fromJson[InterconnectionDeletedInSuperiorInfo](json)(interconnectionDeletedInSuperiorFormat)
        case _ => JsError()
      }
      ).get
  }

  implicit val writes = new Writes[TraceInfo] {
    def writes(ti: TraceInfo): JsValue = {
      TraceInfo.unapply(ti).get._2
    }
  }
}