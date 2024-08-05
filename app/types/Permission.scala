package types

import play.api.libs.json._
import play.api.libs.functional.syntax._
import security.StaticAuthorisationOperation

sealed trait Permission {
  val operations: Set[StaticAuthorisationOperation]
}

object Permission {

  case object DNA_PROFILE_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/profiles|/profiles/.*|/profiles-.*""".r, """GET""".r, "ProfilesRead"),
      StaticAuthorisationOperation("""/profiles""".r, """POST""".r, "ProfilesCreate"),
      StaticAuthorisationOperation("""/profiles-xxx.*""".r, """POST""".r, "AnalysisCreate"),
      StaticAuthorisationOperation("""/profiles-epg""".r, """POST""".r, "EpgCreate"),
      StaticAuthorisationOperation("""/profiles-file""".r, """POST""".r, "FileCreate"),
      StaticAuthorisationOperation("""/profiles-file/file.*""".r, """DELETE""".r, "FileDelete"),
      StaticAuthorisationOperation("""/profiles-file/epg.*""".r, """DELETE""".r, "EpgDelete"),
      StaticAuthorisationOperation("""/profiles-mixture-verification|/profiles-labels""".r, """POST""".r, "ProfileAssociation"),
      StaticAuthorisationOperation("""/strkits.*""".r, """GET""".r, "StrKitsAll"),
      StaticAuthorisationOperation("""/profiledata.*|/profilesdata.*""".r, """GET""".r, "ProfiledataRead"),
      StaticAuthorisationOperation("""/search/profileData.*""".r, """.*""".r, "ProfiledataSearch"),
      StaticAuthorisationOperation("""/uploadImage|/getFilesId|/uploadFile""".r, """.*""".r, "UploadImageAll"),
      StaticAuthorisationOperation("""/categoryTree""".r, """GET""".r, "CategoryTreeRead"),
      StaticAuthorisationOperation("""/locus.*""".r, """GET""".r, "LocusRead"),
      StaticAuthorisationOperation("""/analysistypes""".r, """GET""".r, "AnalysisTypeRead"),
      StaticAuthorisationOperation("""/notifications.*""".r, """.*""".r, "NotificationsAll"),
      StaticAuthorisationOperation("""/inferior/profile""".r, """.*""".r, "UploadProfile"),
      StaticAuthorisationOperation("""/trace.*""".r, """.*""".r, "TraceRead"))
  }
  case object PROFILE_EXPORTER extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/profiledata.*|/profile-export""".r, """POST""".r, "ProfiledataExport"),
      StaticAuthorisationOperation("""/profiledata.*|/get-profile-export.*""".r, """GET""".r, "ProfiledataExport"))
  }
  case object PROFILE_EXPORTER_TO_LIMS extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/profiledata.*|/profile-exportToLims""".r, """POST""".r, "ProfileToLimsdataExport"),
      StaticAuthorisationOperation("""/profiledata.*|/get-alta-file-export.*""".r, """GET""".r, "ProfileToLimsdataExport"),
      StaticAuthorisationOperation("""/profiledata.*|/get-match-file-export.*""".r, """GET""".r, "ProfileToLimsdataExport"))
  }
  case object PROFILE_DATA_CRUD extends Permission {
     override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/profiledata.*|/profilesdata.*""".r, """GET""".r, "ProfiledataRead"),
      StaticAuthorisationOperation("""/search/profileData.*""".r, """.*""".r, "ProfiledataSearch"),
      StaticAuthorisationOperation("""/profiledata-deleted/.*""".r,"""PUT""".r, "ProfiledataDelete",true),
      StaticAuthorisationOperation("""/profiledata.*|/profile-export""".r, """POST""".r, "ProfiledataCreate"),
      StaticAuthorisationOperation("""/profiledata/.*""".r, """PUT""".r, "ProfiledataUpdate"),
      StaticAuthorisationOperation("""/profiledata/modify-category/.*""".r, """PUT""".r, "ProfiledataCategoryModification"),
      StaticAuthorisationOperation("""/profiledata-readonly""".r, """GET""".r, "ProfiledataIsReadOnly"),
      StaticAuthorisationOperation("""/categories|/categoryTree""".r, """GET""".r, "CategoryTreeRead"),
      StaticAuthorisationOperation("""/crimeTypes""".r, """GET""".r, "CrimeTypesRead"),
      StaticAuthorisationOperation("""/bioMaterialTypes""".r, """GET""".r, "BioMaterialTypesRead"),
      StaticAuthorisationOperation("""/uploadImage|/getFilesId|/uploadFile""".r, """.*""".r, "UploadImageAll"),
      StaticAuthorisationOperation("""/laboratory""".r, """GET""".r, "LaboratoryRead"),
      StaticAuthorisationOperation("""/geneticist.*""".r, """GET""".r, "GeneticistRead"),
      StaticAuthorisationOperation("""/trace.*""".r, """.*""".r, "TraceRead")
    )
  }
  case object PROFILE_DATA_SEARCH extends Permission {
     override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/search/profileData.*""".r, """GET""".r, "ProfiledataRead"))
  }
  case object MUTATION_MODELS_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/mutation-models|/mutation-models-types|/mutation-model|/mutation-model-parameters|/mutation-models/default-params""".r, """GET""".r, "MutationModelsGet"),
      StaticAuthorisationOperation("""/mutation-models.*""".r, """DELETE""".r, "MutationModelsDelete"),
      StaticAuthorisationOperation("""/mutation-models.*""".r, """POST""".r, "MutationModelsInsert"),
      StaticAuthorisationOperation("""/mutation-models.*""".r, """PUT""".r, "MutationModelsUpdate")
    )
  }
  case object LABORATORY_CRUD extends Permission {
     override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/laboratory.*""".r, """GET""".r, "LaboratoryRead"),
      StaticAuthorisationOperation("""/laboratory.*""".r, """PUT""".r, "LaboratoryUpdate"),
      StaticAuthorisationOperation("""/laboratory.*""".r, """POST""".r, "LaboratoryCreate", true),
      StaticAuthorisationOperation("""/country""".r, """GET""".r, "CountryRead"),
      StaticAuthorisationOperation("""/provinces.*""".r, """GET""".r, "ProvincesRead"))
  }
  case object GENETICIST_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/laboratory.*""".r, """GET""".r, "LaboratoryRead"),
      StaticAuthorisationOperation("""/geneticist.*""".r, """GET""".r, "GeneticistRead"),
      StaticAuthorisationOperation("""/geneticist.*""".r, """PUT""".r, "GeneticistUpdate"),
      StaticAuthorisationOperation("""/geneticist.*""".r, """POST""".r, "GeneticistCreate"))
  }
  case object ALLELIC_FREQ_DB_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/populationBaseFreq.*""".r, """GET""".r, "PopulationBaseFreqRead"),
      StaticAuthorisationOperation("""/populationBaseFreq.*""".r, """POST""".r, "PopulationBaseFreqCreate"),
      StaticAuthorisationOperation("""/populationBaseFreq.*""".r, """PUT""".r, "PopulationBaseFreqUpdate"))
  }
  case object ALLELIC_FREQ_DB_VIEW extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/populationBaseFreq.*""".r, """GET""".r, "PopulationBaseFreqRead"))
  }
  case object OPERATION_LOG_READ extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/operationLog.*""".r, """.*""".r, "OperationLogAll"),
      StaticAuthorisationOperation("""/operations""".r, """.*""".r, "PermissionAll"))
  }
  case object MATCHES_MANAGER extends Permission{
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/search/matches|/matching|/user-matches|/user-total-matches|/user-matches-group|/user-total-matches-group|/mixture.*|/getByMatchedProfileId|/comparedGenotyfications|/lr.*|/default-scenario|/matching-profile""".r, """.*""".r, "MatchAll"),
      StaticAuthorisationOperation("""/convertHit""".r, """POST""".r, "MatchHit"),
      StaticAuthorisationOperation("""/convertDiscard|/masiveDiscardByGlobalCode|/masiveDiscardByMatchesList""".r, """POST""".r, "MatchDiscard"),
      StaticAuthorisationOperation("""/uploadStatus""".r, """POST""".r, "MatchDiscard"),
      StaticAuthorisationOperation("""/canUploadMatchStatus""".r, """GET""".r, "MatchDiscard"),
      StaticAuthorisationOperation("""/categories|/categoryTree|/categoriesWithProfiles""".r, """GET""".r, "CategoryTreeRead"),
      StaticAuthorisationOperation("""/laboratory.*""".r, """GET""".r, "LaboratoryRead"),
      StaticAuthorisationOperation("""/populationBaseFreq.*""".r, """GET""".r, "PopulationBaseFreqRead"),
      StaticAuthorisationOperation("""/notifications.*|/match-notifications.*""".r, """.*""".r, "NotificationsAll"),
      StaticAuthorisationOperation("""/locus.*""".r, """GET""".r, "LocusRead"),
      StaticAuthorisationOperation("""/analysistypes""".r, """GET""".r, "AnalysisTypeRead"),
      StaticAuthorisationOperation("""/profiles/.*|/profiles-labelsets""".r, """GET""".r, "ProfilesRead"),
      StaticAuthorisationOperation("""/profiledata.*|/profilesdata.*""".r, """GET""".r, "ProfiledataRead"),
      StaticAuthorisationOperation("""/strkits""".r, """GET""".r, "StrKitsAll")
    )
  }
  case object PROTOPROFILE_BULK_UPLOAD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/bulkuploader.*""".r, """POST""".r, "BulkuploaderCreate"),
      StaticAuthorisationOperation("""/bulkupload/step1.*|/protoprofile.*""".r, """GET""".r, "BulkuploaderBatches"),
      StaticAuthorisationOperation("""/protoprofiledata""".r, """POST""".r, "ProtoprofileCreate"),
      StaticAuthorisationOperation("""/protoprofiledata.*""".r, """PUT""".r, "ProtoprofileUpdate"),
      StaticAuthorisationOperation("""/protoprofiles.*""".r, """POST""".r, "ProtoprofileUpdateStatus"),
      StaticAuthorisationOperation("""/notifications.*""".r, """.*""".r, "NotificationsAll"),
      StaticAuthorisationOperation("""/categories|/categoryTree""".r,"""GET""".r, "CategoryTreeRead"),
      StaticAuthorisationOperation("""/crimeTypes""".r, """GET""".r, "CrimeTypesRead"),
      StaticAuthorisationOperation("""/bioMaterialTypes""".r, """GET""".r, "BioMaterialTypesRead"),
      StaticAuthorisationOperation("""/laboratory""".r, """GET""".r, "LaboratoryRead"),
      StaticAuthorisationOperation("""/geneticist.*""".r, """GET""".r, "GeneticistRead"),
      StaticAuthorisationOperation("""/bulkupload.*""".r, """DELETE""".r, "BulkuploaderBatchesDelete"),
      StaticAuthorisationOperation("""/locus.*""".r, """GET""".r, "LocusRead"),
      StaticAuthorisationOperation("""/bulkupload/batches""".r, """GET""".r, "BulkuploaderBatchesSearch"))

  }

  case object PROTOPROFILE_BULK_ACCEPTANCE extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/bulkupload/step2.*|/protoprofiles.*|/bulkupload/protoprofiles.*""".r, """GET""".r, "ProtoprofileRead"),
      StaticAuthorisationOperation("""/protoprofiles.*""".r, """POST""".r, "ProtoprofileUpdateStatus"),
      StaticAuthorisationOperation("""/notifications.*""".r, """.*""".r, "NotificationsAll"),
      StaticAuthorisationOperation("""/bulkupload.*""".r, """DELETE""".r, "BulkuploaderBatchesDelete"),
      StaticAuthorisationOperation("""/motive.*""".r, """GET""".r, "GetMotives"),
      StaticAuthorisationOperation("""/locus.*""".r, """GET""".r, "LocusRead"))
  }
  case object USER_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/users.*""".r, """GET""".r, "UserRead"),
      StaticAuthorisationOperation("""/users""".r, """PUT""".r, "UserUpdate"),
      StaticAuthorisationOperation("""/users/.*""".r, """PUT""".r, "UserUpdateStatus"),
      StaticAuthorisationOperation("""/notifications.*""".r, """.*""".r, "NotificationsAll"))
  }
  case object CATEGORY_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/categories.*""".r, """PUT""".r, "CategoryUpdate"),
      StaticAuthorisationOperation("""/categories.*""".r, """POST""".r, "CategoryCreate"),
      StaticAuthorisationOperation("""/categories.*""".r, """DELETE""".r, "CategoryDelete"),
      StaticAuthorisationOperation("""/categoryTree|/categories|/categoryTreeManualLoading""".r,"""GET""".r, "CategoryTreeRead"),
      StaticAuthorisationOperation("""/group.*""".r,"""POST""".r, "GroupCreate"),
      StaticAuthorisationOperation("""/group.*""".r,"""PUT""".r, "GroupUpdate"),
      StaticAuthorisationOperation("""/group.*""".r,"""DELETE""".r, "GroupDelete"),
      StaticAuthorisationOperation("""/analysistypes""".r, """GET""".r, "AnalysisTypeRead"),
      StaticAuthorisationOperation("""/catmodification""".r, """POST""".r, "CategoryModificationCreate"),
      StaticAuthorisationOperation("""/catmodification""".r, """DELETE""".r, "CategoryModificationDelete"),
      StaticAuthorisationOperation("""/catmodification""".r, """GET""".r, "CategoryModificationRead"),
      StaticAuthorisationOperation("""/catmodifications""".r, """GET""".r, "CategoryModificationRead")
    )
  }
  case object ROLE_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/roles.*""".r, """GET""".r, "RolesRead"),
      StaticAuthorisationOperation("""/roles.*""".r, """POST""".r, "RolesCreate"),
      StaticAuthorisationOperation("""/roles.*""".r, """PUT""".r, "RolesUpdate"),
      StaticAuthorisationOperation("""/roles.*""".r, """DELETE""".r, "RolesDelete"),
      StaticAuthorisationOperation("""/permission.*|/operation.*""".r, """.*""".r, "PermissionAll"))
  }
  case object PEDIGREE_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
        StaticAuthorisationOperation("""/pedigree|/pedigree/by/.*|/pedigree/court-cases|/pedigree/caseTypes|/pedigree/total-court-cases|/pedigree/full.*|/pedigree/genogram.*|/pedigree/editable.*|/pedigree/deleteable.*|/mutation-models/active""".r, """.*""".r, "PedigreeAll"),
        StaticAuthorisationOperation("""/pedigree/court-cases.*|/pedigree/update-metadata|/pedigree/remove-metadata""".r,"""PUT""".r, "PedigreeCourtCaseUpdate"),
        StaticAuthorisationOperation("""/pedigree/create-court-cases|/pedigree/metadata|/pedigree-matches-courtcase/search""".r,"""POST""".r, "PedigreeCourtCaseCreate"),
        StaticAuthorisationOperation("""/pedigree/create-genogram""".r,"""POST""".r, "PedigreeGenogramCreate"),
        StaticAuthorisationOperation("""/pedigree/status""".r,"""POST""".r, "PedigreeChangeStatus"),
        StaticAuthorisationOperation("""/pedigree/courtCaseStatus""".r,"""POST""".r, "CourtCaseChangeStatus"),
        StaticAuthorisationOperation("""/pedigreeMatches.*|/pedigree/pedigree-coincidencia|/pedigree/perfil-coincidencia|/get-pedigreeMatches-export""".r,""".*""".r, "PedigreeMatchesRead"),
        StaticAuthorisationOperation("""/search/profileDataForPedigree|/filter/profileDataForPedigree|/filter/profileDataForPedigree/count|/pedigree/profiles|/pedigree/total-alias|/pedigrees-profiles/total-association""".r,"""GET""".r, "PedigreeProfileAsocRead"),
        StaticAuthorisationOperation("""/notifications.*|/match-notifications.*""".r, """.*""".r, "NotificationsAll"),
        StaticAuthorisationOperation("""/crimeTypes""".r, """GET""".r, "CrimeTypesRead"),
        StaticAuthorisationOperation("""/pedigree/caseTypes.*""".r, """GET""".r, "CaseTypeRead"),
        StaticAuthorisationOperation("""/geneticist.*""".r, """GET""".r, "GeneticistRead"),
        StaticAuthorisationOperation("""/analysistypes""".r, """GET""".r, "AnalysisTypeRead"),
        StaticAuthorisationOperation("""/pedigree/scenario""".r, """POST""".r, "PedigreeScenarioCreate"),
        StaticAuthorisationOperation("""/pedigree/scenario""".r, """PUT""".r, "PedigreeScenarioUpdate"),
        StaticAuthorisationOperation("""/pedigree/scenario.*""".r, """GET""".r, "PedigreeScenarioRead"),
        StaticAuthorisationOperation("""/pedigree/scenario-status""".r, """POST""".r, "PedigreeScenarioStatusChange"),
        StaticAuthorisationOperation("""/pedigree/scenario-validate""".r, """POST""".r, "PedigreeScenarioConfirm"),
        StaticAuthorisationOperation("""/pedigree/discard|/pedigreeMatches/masiveDiscardByGroup""".r, """POST""".r, "PedigreeMatchDiscard"),
        StaticAuthorisationOperation("""/pedigree/lr""".r, """POST""".r, "PedigreeScenarioLR"),
        StaticAuthorisationOperation("""/locus/ranges""".r, """GET""".r, "GetRangesLocus"),
        StaticAuthorisationOperation("""/court-case-profiles|/court-case-profiles/total|/court-case-profiles-node-association|/pedigree/filtrar-alias|/grouped-profiles|/grouped-profiles/total-grouped|/grouped-profiles/validation""".r, """GET""".r, "PedigreeCourtCaseProfilesGet"),
        StaticAuthorisationOperation("""/court-case-profiles|/collapsing|/collapsing-manual""".r, """POST""".r, "PedigreeCourtCaseProfilesPost"),
        StaticAuthorisationOperation("""/court-case-profiles|/pedigree/remove-metadata|/grouped-profiles/disassociate""".r, """PUT""".r, "PedigreeCourtCaseProfilesDelete"),
        StaticAuthorisationOperation("""/batch-modal""".r, """GET""".r, "GetBatchModal"),
        StaticAuthorisationOperation("""/batch-modal-import""".r, """POST""".r, "PostBatchModal"),
        StaticAuthorisationOperation("""/court-case-pedigrees|/court-case-pedigrees/total""".r, """POST""".r, "PedigreeCourtCaseGet"),
        StaticAuthorisationOperation("""/pedigree/save""".r, """POST""".r, "PedigreeMetadataSave"),
        StaticAuthorisationOperation("""/pedigree/delete""".r, """POST""".r, "PedigreeDelete"),
        StaticAuthorisationOperation("""/court-case/deleteable.*""".r, """GET""".r, "CourCaseDeleteable"),
        StaticAuthorisationOperation("""/court-case/closeable.*""".r, """GET""".r, "CourtCaseCloseable"),
        StaticAuthorisationOperation("""/collapsing/partial|/collapsing/full""".r, """DELETE""".r, "CollapsingDiscard"),
        StaticAuthorisationOperation("""/collapsing/groups""".r, """POST""".r, "CollapsingCreate"),
        StaticAuthorisationOperation("""/pedcheck""".r, """GET""".r, "PedigreeConsistencyView"),
        StaticAuthorisationOperation("""/pedcheck""".r, """POST""".r, "PedigreeConsistencyGenerate")
    )
  }
  
  case object BIO_MAT_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/bioMaterialTypes""".r, """GET""".r, "BioMaterialTypesRead"),
      StaticAuthorisationOperation("""/bioMaterialTypes.*""".r, """POST""".r, "BioMaterialTypesCreate"),
      StaticAuthorisationOperation("""/bioMaterialTypes.*""".r, """PUT""".r, "BioMaterialTypesUpdate"),
      StaticAuthorisationOperation("""/bioMaterialTypes.*""".r, """DELETE""".r, "BioMaterialTypesDelete")
    )
  }

  case object SCENARIO_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/scenarios/update""".r, """POST""".r, "ScenarioUpdate"),
      StaticAuthorisationOperation("""/scenarios/validate""".r, """POST""".r, "ScenarioValidate"),
      StaticAuthorisationOperation("""/scenarios/create""".r, """POST""".r, "ScenarioCreate"),
      StaticAuthorisationOperation("""/scenarios/search|/scenarios/get""".r, """.*""".r, "ScenarioRead"),
      StaticAuthorisationOperation("""/scenarios/delete""".r, """PUT""".r, "ScenarioDelete"),
      StaticAuthorisationOperation("""/scenarios/ncorrection""".r, """POST""".r, "ScenarioNCorrection"),
      StaticAuthorisationOperation("""/search/matches|/matching|/user-matches|/mixture.*|/getByMatchedProfileId|/comparedGenotyfications|/lr.*|/default-scenario|/matchesForScenario""".r, """.*""".r, "MatchAll"),
      StaticAuthorisationOperation("""/profiles|/profiles/.*|/profiles-.*""".r, """GET""".r, "ProfilesRead"),
      StaticAuthorisationOperation("""/profiledata.*""".r, """GET""".r, "ProfiledataRead"),
      StaticAuthorisationOperation("""/laboratory/.*""".r, """GET""".r, "LaboratoryRead"),
      StaticAuthorisationOperation("""/categoryTree""".r,"""GET""".r, "CategoryTreeRead"),
      StaticAuthorisationOperation("""/populationBaseFreq.*""".r, """GET""".r, "PopulationBaseFreqRead")
    )
  }

  case object LOCUS_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/locus.*""".r, """POST""".r, "LocusCreate"),
      StaticAuthorisationOperation("""/locus.*""".r, """GET""".r, "LocusRead"),
      StaticAuthorisationOperation("""/locus.*""".r, """DELETE""".r, "LocusDelete"),
      StaticAuthorisationOperation("""/analysistypes""".r, """GET""".r, "AnalysisTypeRead")
    )
  }

  case object LOCUS_UPDATE extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/locus.*""".r, """PUT""".r, "LocusUpdate")
    )
  }

  case object ADD_MANUAL_LOCUS extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set()
  }

  case object KIT_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/locus.*""".r, """GET""".r, "LocusRead"),
      StaticAuthorisationOperation("""/strkit.*""".r, """GET""".r, "StrKitsAll"),
      StaticAuthorisationOperation("""/strkit.*""".r, """POST""".r, "StrKitCreate"),
      StaticAuthorisationOperation("""/strkit.*""".r, """PUT""".r, "StrKitUpdate"),
      StaticAuthorisationOperation("""/strkit.*""".r, """DELETE""".r, "StrKitDelete"),
      StaticAuthorisationOperation("""/analysistypes""".r, """GET""".r, "AnalysisTypeRead")
    )
  }

  case object MOTIVE_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/motive-types.*""".r, """GET""".r, "MotiveTypesRead"),
      StaticAuthorisationOperation("""/motive.*""".r, """GET""".r, "GetMotives"),
      StaticAuthorisationOperation("""/motive.*""".r, """POST""".r, "InsertMotives"),
      StaticAuthorisationOperation("""/motive.*""".r, """DELETE""".r, "DeleteMotives"),
      StaticAuthorisationOperation("""/motive""".r, """PUT""".r, "UpdateMotives")
    )
  }

  case object SUP_INS_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/categories-mapping""".r,"""GET""".r, "GetCatMapping"),
      StaticAuthorisationOperation("""/categories-mapping""".r,"""PUT""".r, "PutCatMapping"),
      StaticAuthorisationOperation("""/connections""".r,"""PUT""".r, "PutSupInstConf"),
      StaticAuthorisationOperation("""/connections""".r,"""GET""".r, "GetSupInstConf"),
      StaticAuthorisationOperation("""/connections/status""".r,"""GET""".r, "CheckInstConf"),
      StaticAuthorisationOperation("""/superior/category-tree-consumer""".r,"""GET""".r, "GetCategoryTreeCombo"),
      StaticAuthorisationOperation("""/connection""".r,"""POST""".r, "PostConnection")
    )
  }
  case object INSTANCE_INTERCONNECTION extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/status""".r,"""GET""".r, "CheckInstConf"),
      StaticAuthorisationOperation("""/superior/connection""".r,"""POST""".r, "PostConnection"),
      StaticAuthorisationOperation("""/superior/category-tree-combo""".r,"""GET""".r, "GetCategoryTreeCombo"),
      StaticAuthorisationOperation("""/superior/profile|/interconnection/file""".r,"""POST""".r, "PostIIProfile"),
      StaticAuthorisationOperation("""/superior/profile.*""".r,"""DELETE""".r, "DeleteIIProfile"),
      StaticAuthorisationOperation("""/inferior/match/.*|/interconection/match/status.*""".r,"""POST""".r, "ReceiveMatchFromSuperior"),
      StaticAuthorisationOperation("""/interconection/match/status.*""".r,"""POST""".r, "MatchUpdateInterconnection")
    )
  }
  case object INF_INS_CRUD extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/instances/inferior""".r,"""GET""".r, "GetInfIns"),
      StaticAuthorisationOperation("""/instances/inferior""".r,"""PUT""".r, "PutInfIns"),
      StaticAuthorisationOperation("""/instances/inferior/status""".r,"""GET""".r, "GetInfInsStatus")
    )
  }
  case object INTERCON_NOTIF extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set.empty
  }
  case object IMP_PERF_INS extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/superior/profile/approval.*""".r,"""GET""".r, "GetPenProfApp"),
      StaticAuthorisationOperation("""/superior/profile/approval""".r,"""POST""".r, "PostPenProfApp"),
      StaticAuthorisationOperation("""/superior/profile/approval""".r,"""DELETE""".r, "DelPenProfApp")
    )
  }
  case object PROFILE_COMPARISON extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/profile-comparison""".r,"""GET""".r, "GetProfileComparison"),
      StaticAuthorisationOperation("""/modify-forensic-category""".r,"""GET""".r, "GetProfileComparison")
      // TODO: Change GetProfileComparison to a different description key and then...
      // TODO: Update translation.json (public/locales/es-AR/translation.json)
    )
  }
  case object LOGIN_SIGNUP extends Permission {
    override val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/signup.*""".r, """.*""".r, "UserCreate"),
      StaticAuthorisationOperation("""/clear-password.*""".r, """.*""".r, "UserCreate"),
      StaticAuthorisationOperation("""/disclaimer.*""".r, """.*""".r, "UserCreate"),
      StaticAuthorisationOperation("""/login""".r, """POST""".r, "Login")
    )
  }

  case object REPORTING_VIEW extends Permission {
    override  val operations: Set[StaticAuthorisationOperation] = Set(
      StaticAuthorisationOperation("""/reportes.*""".r, """GET""".r, "ReportingRead")
    )
  }


  val list: Set[Permission] = Set(
    DNA_PROFILE_CRUD,
    PROFILE_EXPORTER,
    PROFILE_EXPORTER_TO_LIMS,
    PROFILE_DATA_CRUD,
    LABORATORY_CRUD,
    GENETICIST_CRUD,
    ALLELIC_FREQ_DB_CRUD,
    ALLELIC_FREQ_DB_VIEW,
    OPERATION_LOG_READ,
    MATCHES_MANAGER,
    PROTOPROFILE_BULK_UPLOAD,
    PROTOPROFILE_BULK_ACCEPTANCE,
    USER_CRUD,
    CATEGORY_CRUD,
    ROLE_CRUD,
    PEDIGREE_CRUD,
    BIO_MAT_CRUD,
    SCENARIO_CRUD,
    LOCUS_CRUD,
    LOCUS_UPDATE,
    KIT_CRUD,
    SUP_INS_CRUD,
    INF_INS_CRUD,
    INTERCON_NOTIF,
    INSTANCE_INTERCONNECTION,
    IMP_PERF_INS,
    MOTIVE_CRUD,
    ADD_MANUAL_LOCUS,
    REPORTING_VIEW,
    MUTATION_MODELS_CRUD,
    PROFILE_COMPARISON
  )

  def fromString(value: String): Option[Permission] = Permission.list.find(_.toString == value)

  implicit val permissionWrites: Writes[Permission] = new Writes[Permission] {
    def writes(permission: Permission): JsValue = JsString(permission.toString())
  }
  
  implicit val permissionRead: Reads[Permission] = new Reads[Permission] {
    def reads(json: JsValue): JsResult[Permission] = json match {
        case JsString(s) => {
          try {
            JsSuccess(Permission.fromString(s).get)
          } catch {
            case _: NoSuchElementException =>
              JsError(s"Permission enumeration expected")
          }
        }
        case _ => JsError("String value expected")
      }
  }
}
