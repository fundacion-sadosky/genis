package configdata

import javax.inject.{Inject, Singleton}
import matching.{Algorithm, Stringency}
import models.Tables
import scala.concurrent.{ExecutionContext, Future}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import types.AlphanumericId

trait CategoryRepository {
  def listGroupsAndCategories: Future[Seq[(Group, Option[Category])]]
  def listGroupsAndCategoriesManualLoading: Future[Seq[(Group, Option[Category])]]
  def listCategories: Future[Seq[FullCategory]]
  def listCategoriesWithProfiles: Future[List[Category]]
  def listConfigurations: Future[Seq[CategoryConfigurationExport]]
  def listAssociations: Future[Seq[CategoryAssociationExport]]
  def listAlias: Future[Seq[CategoryAliasExport]]
  def listMatchingRules: Future[Seq[CategoryMatchingExport]]

  def addCategory(cat: Category): Future[AlphanumericId]
  def updateCategory(category: Category): Future[Int]
  def updateFullCategory(category: FullCategory): Future[Either[String, AlphanumericId]]
  def removeCategory(categoryId: AlphanumericId): Future[Either[String, Unit]]
  def removeAllCategories(): Future[Int]

  def addGroup(group: Group): Future[AlphanumericId]
  def updateGroup(group: Group): Future[Int]
  def removeGroup(groupId: AlphanumericId): Future[Int]
  def removeAllGroups(): Future[Int]

  def insertOrUpdateMapping(mappings: CategoryMappingList): Future[Either[String, Unit]]
  def listCategoriesMapping: Future[List[FullCategoryMapping]]
  def getCategoriesMappingById(id: AlphanumericId): Future[Option[String]]
  def getCategoriesMappingReverseById(id: AlphanumericId): Future[Option[AlphanumericId]]
  def deleteCategoryMappingById(id: String): Future[Either[String, String]]

  def addCategoryModification(from: AlphanumericId, to: AlphanumericId): Future[Int]
  def categoryModificationExists(from: AlphanumericId, to: AlphanumericId): Future[Boolean]
  def getCategoryModificationsAllowed(categoryId: AlphanumericId): Future[Seq[AlphanumericId]]
  def removeCategoryModification(from: AlphanumericId, to: AlphanumericId): Future[Int]
  def getAllCategoryModificationsAllowed: Future[Seq[(AlphanumericId, AlphanumericId)]]
}

@Singleton
class SlickCategoryRepository @Inject()(db: Database)(implicit ec: ExecutionContext) extends CategoryRepository {

  private val groupTable      = Tables.Group
  private val categoryTable   = Tables.Category
  private val aliasTable      = Tables.CategoryAlias
  private val assocTable      = Tables.CategoryAssociation
  private val matchingTable   = Tables.CategoryMatching
  private val configTable     = Tables.CategoryConfiguration
  private val mappingTable    = Tables.CategoryMapping
  private val modTable        = Tables.CategoryModifications
  private val analysisTable   = Tables.AnalysisType
  private val profileDataTable = Tables.ProfileDataCategory

  // ─── Reads ────────────────────────────────────────────────────────────────

  override def listGroupsAndCategories: Future[Seq[(Group, Option[Category])]] = {
    val q = groupTable.joinLeft(categoryTable).on(_.id === _.group).sortBy(_._1.name)
    db.run(q.result).map(_.map { case (gr, maybeCat) =>
      val group = Group(AlphanumericId(gr.id), gr.name, gr.description)
      val cat   = maybeCat.map(c => Category(AlphanumericId(c.id), AlphanumericId(gr.id), c.name, c.isReference, c.description))
      (group, cat)
    })
  }

  override def listGroupsAndCategoriesManualLoading: Future[Seq[(Group, Option[Category])]] = {
    val q = groupTable.joinLeft(categoryTable.filter(_.allowManualLoad === true)).on(_.id === _.group).sortBy(_._1.name)
    db.run(q.result).map(_.map { case (gr, maybeCat) =>
      val group = Group(AlphanumericId(gr.id), gr.name, gr.description)
      val cat   = maybeCat.map(c => Category(AlphanumericId(c.id), AlphanumericId(gr.id), c.name, c.isReference, c.description))
      (group, cat)
    })
  }

  override def listCategories: Future[Seq[FullCategory]] = {
    val action = for {
      cats     <- categoryTable.result
      aliases  <- aliasTable.result
      assocs   <- assocTable.result
      matches  <- matchingTable.joinLeft(analysisTable).on(_.`type` === _.id).result
      confs    <- configTable.result
    } yield {
      cats.map { cat =>
        val thisAliases = aliases.filter(_.category == cat.id).map(_.alias)
        val thisAssocs  = assocs.filter(_.category == cat.id).map { x =>
          CategoryAssociation(x.`type`, AlphanumericId(x.categoryRelated), x.mismatchs)
        }
        val thisConf    = confs.filter(_.category == cat.id).map { x =>
          x.`type` -> CategoryConfiguration(x.collectionUri, x.draftUri,
            x.minLocusPerProfile, x.maxOverageDeviatedLoci, x.maxAllelesPerLocus, x.multiallelic)
        }.toMap
        val thisRules   = matches.filter(_._1.category == cat.id).map {
          case (cmr, maybeAt) =>
            MatchingRule(cmr.`type`, AlphanumericId(cmr.categoryRelated),
              Stringency.withName(cmr.minimumStringency), cmr.failOnMatch.getOrElse(false),
              cmr.forwardToUpper.getOrElse(false), Algorithm.withName(cmr.matchingAlgorithm),
              cmr.minLocusMatch, cmr.mismatchsAllowed, cmr.considerForN,
              maybeAt.exists(_.mitochondrial))
        }
        FullCategory(
          AlphanumericId(cat.id), cat.name, cat.description, AlphanumericId(cat.group),
          cat.isReference, cat.filiationData, cat.replicate, cat.pedigreeAssociation, cat.allowManualLoading,
          thisConf, thisAssocs.toSeq, thisAliases.toSeq, thisRules.toSeq, Some(cat.tipo))
      }
    }
    db.run(action)
  }

  override def listCategoriesWithProfiles: Future[List[Category]] = {
    val q = categoryTable.join(profileDataTable).on(_.id === _.category).map(_._1).distinct
    db.run(q.result).map(_.map(c =>
      Category(AlphanumericId(c.id), AlphanumericId(c.group), c.name, c.isReference, c.description)
    ).toList)
  }

  override def listConfigurations: Future[Seq[CategoryConfigurationExport]] =
    db.run(configTable.result).map(_.map(r =>
      CategoryConfigurationExport(r.id, r.category, r.`type`, r.collectionUri, r.draftUri,
        r.minLocusPerProfile, r.maxOverageDeviatedLoci, r.maxAllelesPerLocus, r.multiallelic)))

  override def listAssociations: Future[Seq[CategoryAssociationExport]] =
    db.run(assocTable.result).map(_.map(r =>
      CategoryAssociationExport(r.id, r.category, r.categoryRelated, r.mismatchs, r.`type`)))

  override def listAlias: Future[Seq[CategoryAliasExport]] =
    db.run(aliasTable.result).map(_.map(r =>
      CategoryAliasExport(r.alias, r.category)))

  override def listMatchingRules: Future[Seq[CategoryMatchingExport]] =
    db.run(matchingTable.result).map(_.map(r =>
      CategoryMatchingExport(r.id, r.category, r.categoryRelated, r.priority,
        r.minimumStringency, r.failOnMatch, r.forwardToUpper, r.matchingAlgorithm,
        r.minLocusMatch, r.mismatchsAllowed, r.`type`, r.considerForN)))

  // ─── Category writes ───────────────────────────────────────────────────────

  override def addCategory(cat: Category): Future[AlphanumericId] = {
    val row = Tables.CategoryRow(cat.id.text, cat.group.text, cat.name, cat.isReference, cat.description)
    db.run(categoryTable += row).map(_ => cat.id)
  }

  override def updateCategory(category: Category): Future[Int] =
    db.run(categoryTable.filter(_.id === category.id.text)
      .map(c => (c.name, c.description))
      .update((category.name, category.description)))

  override def updateFullCategory(cat: FullCategory): Future[Either[String, AlphanumericId]] = {
    val rulesWithoutSelf = cat.matchingRules.filterNot(_.categoryRelated == cat.id)

    val action = for {
      _ <- aliasTable.filter(_.category === cat.id.text).delete
      _ <- assocTable.filter(_.category === cat.id.text).delete
      _ <- matchingTable.filter(_.category === cat.id.text).delete
      _ <- configTable.filter(_.category === cat.id.text).delete
      _ <- DBIO.sequence(rulesWithoutSelf.map { mr =>
             matchingTable.filter(r => r.category === mr.categoryRelated.text && r.categoryRelated === cat.id.text).delete
           })
      _ <- categoryTable.filter(_.id === cat.id.text)
             .map(c => (c.name, c.description, c.filiationData, c.replicate, c.pedigreeAssoc, c.isReference, c.allowManualLoad))
             .update((cat.name, cat.description, cat.filiationDataRequired, cat.replicate, cat.pedigreeAssociation, cat.isReference, cat.manualLoading))
      _ <- aliasTable ++= cat.aliases.map(a => Tables.CategoryAliasRow(a, cat.id.text))
      _ <- assocTable ++= cat.associations.map { a =>
             Tables.CategoryAssociationRow(0L, cat.id.text, a.categoryRelated.text, a.mismatches, a.`type`)
           }
      _ <- matchingTable ++= cat.matchingRules.map { mr =>
             Tables.CategoryMatchingRow(0L, cat.id.text, mr.categoryRelated.text, 1,
               mr.minimumStringency.toString, Some(mr.failOnMatch), Some(mr.forwardToUpper),
               mr.matchingAlgorithm.toString, mr.minLocusMatch, mr.mismatchsAllowed, mr.`type`, mr.considerForN)
           }
      _ <- configTable ++= cat.configurations.map { case (aType, conf) =>
             Tables.CategoryConfigurationRow(0L, cat.id.text, aType, conf.collectionUri, conf.draftUri,
               conf.minLocusPerProfile, conf.maxOverageDeviatedLoci, conf.maxAllelesPerLocus, conf.multiallelic)
           }
      _ <- matchingTable ++= rulesWithoutSelf.map { mr =>
             Tables.CategoryMatchingRow(0L, mr.categoryRelated.text, cat.id.text, 1,
               mr.minimumStringency.toString, Some(mr.failOnMatch), Some(mr.forwardToUpper),
               mr.matchingAlgorithm.toString, mr.minLocusMatch, mr.mismatchsAllowed, mr.`type`, mr.considerForN)
           }
    } yield Right(cat.id): Either[String, AlphanumericId]

    db.run(action.transactionally).recover { case e => Left(e.getMessage) }
  }

  override def removeCategory(categoryId: AlphanumericId): Future[Either[String, Unit]] = {
    val action = matchingTable.filter(_.category === categoryId.text).result.flatMap { rules =>
      val rulesWithoutSelf = rules.filterNot(_.categoryRelated == categoryId.text)
      for {
        _ <- aliasTable.filter(_.category === categoryId.text).delete
        _ <- assocTable.filter(_.category === categoryId.text).delete
        _ <- matchingTable.filter(_.category === categoryId.text).delete
        _ <- configTable.filter(_.category === categoryId.text).delete
        _ <- DBIO.sequence(rulesWithoutSelf.map { mr =>
               matchingTable.filter(r => r.category === mr.categoryRelated && r.categoryRelated === categoryId.text).delete
             })
        _ <- mappingTable.filter(_.id === categoryId.text).delete
        _ <- categoryTable.filter(_.id === categoryId.text).delete
      } yield Right(()): Either[String, Unit]
    }
    db.run(action.transactionally).recover { case e => Left(e.getMessage) }
  }

  override def removeAllCategories(): Future[Int] = {
    val action = for {
      _ <- matchingTable.delete
      _ <- assocTable.delete
      _ <- aliasTable.delete
      _ <- configTable.delete
      _ <- mappingTable.delete
      _ <- modTable.delete
      n <- categoryTable.delete
    } yield n
    db.run(action.transactionally)
  }

  // ─── Group writes ─────────────────────────────────────────────────────────

  override def addGroup(group: Group): Future[AlphanumericId] = {
    val row = Tables.GroupRow(group.id.text, group.name, group.description)
    db.run(groupTable += row).map(_ => group.id)
  }

  override def updateGroup(group: Group): Future[Int] =
    db.run(groupTable.filter(_.id === group.id.text).map(g => (g.name, g.description)).update((group.name, None)))

  override def removeGroup(groupId: AlphanumericId): Future[Int] =
    db.run(groupTable.filter(_.id === groupId.text).delete)

  override def removeAllGroups(): Future[Int] =
    db.run(groupTable.delete)

  // ─── Mapping ──────────────────────────────────────────────────────────────

  override def insertOrUpdateMapping(mappings: CategoryMappingList): Future[Either[String, Unit]] = {
    val actions = DBIO.sequence(mappings.categoryMappingList.map { m =>
      mappingTable.insertOrUpdate(Tables.CategoryMappingRow(m.id.text, m.idSuperior))
    })
    db.run(actions.transactionally).map(_ => Right(())).recover { case e => Left(e.getMessage) }
  }

  override def listCategoriesMapping: Future[List[FullCategoryMapping]] = {
    val q = categoryTable
      .filter(c => c.replicate === true && c.pedigreeAssoc === false)
      .join(groupTable).on(_.group === _.id)
      .joinLeft(mappingTable).on(_._1.id === _.id)
      .sortBy(_._1._2.name)
    db.run(q.result).map(_.map { case ((cat, grp), maybeMap) =>
      FullCategoryMapping(AlphanumericId(cat.id), cat.name, grp.name, maybeMap.map(_.idSuperior).getOrElse(""))
    }.toList)
  }

  override def getCategoriesMappingById(id: AlphanumericId): Future[Option[String]] =
    db.run(mappingTable.filter(_.id === id.text).result.headOption).map {
      case None                                        => None
      case Some(Tables.CategoryMappingRow(_, ""))      => None
      case Some(Tables.CategoryMappingRow(_, superior)) => Some(superior)
    }

  override def getCategoriesMappingReverseById(id: AlphanumericId): Future[Option[AlphanumericId]] =
    db.run(mappingTable.filter(_.idSuperior === id.text).result.headOption).map(_.map(r => AlphanumericId(r.id)))

  override def deleteCategoryMappingById(id: String): Future[Either[String, String]] =
    db.run(mappingTable.filter(_.id === id).delete).map(_ => Right(id)).recover { case e => Left(e.getMessage) }

  // ─── Category modifications ───────────────────────────────────────────────

  override def addCategoryModification(from: AlphanumericId, to: AlphanumericId): Future[Int] =
    db.run(modTable += Tables.CategoryModificationsRow(from.text, to.text))

  override def categoryModificationExists(from: AlphanumericId, to: AlphanumericId): Future[Boolean] =
    db.run(modTable.filter(r => r.from === from.text && r.to === to.text).exists.result)

  override def getCategoryModificationsAllowed(categoryId: AlphanumericId): Future[Seq[AlphanumericId]] =
    db.run(modTable.filter(_.from === categoryId.text).map(_.to).result).map(_.map(AlphanumericId(_)))

  override def removeCategoryModification(from: AlphanumericId, to: AlphanumericId): Future[Int] =
    db.run(modTable.filter(r => r.from === from.text && r.to === to.text).delete)

  override def getAllCategoryModificationsAllowed: Future[Seq[(AlphanumericId, AlphanumericId)]] =
    db.run(modTable.map(r => (r.from, r.to)).result).map(_.map { case (f, t) => (AlphanumericId(f), AlphanumericId(t)) })
}
