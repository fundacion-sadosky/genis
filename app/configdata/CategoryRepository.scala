package configdata

import java.sql.SQLException
import javax.inject.{Inject, Singleton}
import matching.{Algorithm, Stringency}
import models.Tables
import models.Tables._
import play.api.Application
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.i18n.{Messages, MessagesApi}
import types.AlphanumericId
import util.{DefaultDb, Transaction}

import scala.concurrent.Future
import scala.slick.jdbc.{StaticQuery => Q}
import Q.interpolation
import scala.slick.jdbc.StaticQuery.staticQueryToInvoker

abstract class CategoryRepository extends DefaultDb with Transaction {

  def listGroupsAndCategories: Future[Seq[(Group, Option[Category])]]
  def listGroupsAndCategoriesManualLoading: Future[Seq[(Group, Option[Category])]]
  def listCategories: Future[Seq[FullCategory]]

  def listCategoriesWithProfiles: Future[List[Category]]

  def addCategory(cat: Category): Future[AlphanumericId]
  def updateCategory(category: Category): Future[Int]
  def removeCategory(categoryId: AlphanumericId): Future[Int]

  def updateFullCategory(category: FullCategory)(implicit session: Session): Either[String, AlphanumericId]

  def deleteAssociations(categoryId: AlphanumericId)(implicit session: Session): Either[String, AlphanumericId]
  def deleteAliases(categoryId: AlphanumericId)(implicit session: Session): Either[String, AlphanumericId]
  def deleteMatchingRules(categoryId: AlphanumericId)(implicit session: Session): Either[String, AlphanumericId]
  def deleteMatchingRule(categoryId: AlphanumericId, categoryRelatedId: AlphanumericId)(implicit session: Session): Either[String, AlphanumericId]
  def deleteConfigurations(categoryId: AlphanumericId)(implicit session: Session): Either[String, AlphanumericId]

  def addAssociations(categoryId: AlphanumericId, associations: Seq[CategoryAssociation])(implicit s: Session): Either[String, AlphanumericId]
  def addAliases(categoryId: AlphanumericId, alias: Seq[String])(implicit s: Session): Either[String, AlphanumericId]
  def addMatchingRules(categoryId: AlphanumericId, matchingRules: Seq[MatchingRule])(implicit s: Session): Either[String, AlphanumericId]
  def addConfigurations(categoryId: AlphanumericId, configurations: Map[Int, CategoryConfiguration])(implicit session: Session): Either[String, AlphanumericId]

  def addGroup(group: Group): Future[AlphanumericId]
  def updateGroup(group: Group): Future[Int]
  def removeGroup(groupId: AlphanumericId): Future[Int]

  def insertOrUpdateMapping(categoryMapping: CategoryMappingList): Future[Either[String, Unit]]
  def listCategoriesMapping: Future[List[FullCategoryMapping]]
  def getCategoriesMappingById(id:AlphanumericId): Future[Option[String]]
  def getCategoriesMappingReverseById(id:AlphanumericId): Future[Option[AlphanumericId]]
  def deleteCategoryMappingById(id: String): Future[Either[String, String]]
}

@Singleton
class SlickCategoryRepository @Inject() (implicit app: Application, messagesApi: MessagesApi) extends CategoryRepository {

  implicit val messages: Messages = messagesApi.preferred(Seq.empty)

  val groups: TableQuery[Tables.Group] = Tables.Group
  val categories: TableQuery[Tables.Category] = Tables.Category

  val categoriesAlias: TableQuery[Tables.CategoryAlias] = Tables.CategoryAlias
  val categoryAssoc: TableQuery[Tables.CategoryAssociation] = Tables.CategoryAssociation
  val categoryMatching: TableQuery[Tables.CategoryMatching] = Tables.CategoryMatching
  val categoryConfiguration: TableQuery[Tables.CategoryConfiguration] = Tables.CategoryConfiguration
  val categoryMappingTable: TableQuery[Tables.CategoryMapping] = Tables.CategoryMapping

  val analysisTypes: TableQuery[Tables.AnalysisType] = Tables.AnalysisType

  val profilesData: TableQuery[Tables.ProfileData] = Tables.ProfileData // Tables.ProtoProfileData

  private def queryGetCategoriesWithProfiles() = for (
    (category,pd) <- categories join profilesData on (_.id === _.category) //groupBy(row=>(row._1.id,row._1.name,row._1.isReference,row._1.description))
  ) yield (category)

  override def listCategoriesWithProfiles: Future[List[Category]] = Future{
  DB.withSession { implicit session =>
    queryGetCategoriesWithProfiles().list.map(c=>
      Category(AlphanumericId(c.id),AlphanumericId(c.group),c.name,c.isReference,c.description)
    )}
  }

  val categoryQuery = Compiled(for (
    (category) <- categories
  ) yield category)

  val confQuery = Compiled(for (conf <- categoryConfiguration) yield conf)

  val aliasQuery = Compiled(for (alias <- categoriesAlias) yield alias)

  val assocQuery = Compiled(for (assoc <- categoryAssoc) yield assoc)

  val matchQuery = Compiled(for ((mtch, at) <- categoryMatching leftJoin analysisTypes on (_.`type` === _.id)) yield (mtch, at))

  val mappingQuery = Compiled(for (mapping <- categoryMappingTable) yield mapping)

  private def queryDefineGetAlias(category: Column[String]) = for (
    alias <- categoriesAlias if alias.category === category
  ) yield alias

  val queryGetAlias = Compiled(queryDefineGetAlias _)

  private def queryDefineGetConfigurations(category: Column[String]) = for (
    conf <- categoryConfiguration if conf.category === category
  ) yield conf

  val queryGetConfigurations = Compiled(queryDefineGetConfigurations _)

  private def queryDefineGetCategoryUpd(category: Column[String]) = for (
    cat <- categories if cat.id === category
  ) yield (cat.name, cat.description)

  val queryGetCategoryUpd = Compiled(queryDefineGetCategoryUpd _)

  private def queryDefineFullCategoryUpd(category: Column[String]) = for (
    cat <- categories if (cat.id === category)
  ) yield (cat.name, cat.description, cat.filiationData, cat.replicate, cat.pedigreeAssociation, cat.isReference, cat.allowManualLoading)

  val queryFullCategoryUpd = Compiled(queryDefineFullCategoryUpd _)

  private def queryDefineGetCategory(category: Column[String]) = for (
    cat <- categories if (cat.id === category)
  ) yield (cat)

  val queryGetCategory = Compiled(queryDefineGetCategory _)

  private def queryDefineGetAssocs(category: Column[String]) = for (
    assoc <- categoryAssoc if (assoc.category === category)
  ) yield (assoc)

  val queryGetAssocs = Compiled(queryDefineGetAssocs _)

  private def queryDefineGetMatchRules(category: Column[String]) = for (
    mr <- categoryMatching if (mr.category === category)
  ) yield (mr)

  val queryGetMatchRules = Compiled(queryDefineGetMatchRules _)

  private def queryDefineGetMatchRule(category: Column[String], categoryRelated: Column[String]) = for (
    mr <- categoryMatching if (mr.category === category && mr.categoryRelated === categoryRelated)
  ) yield (mr)

  val queryGetMatchRule = Compiled(queryDefineGetMatchRule _)

  private def queryDefineUpdateGroup(groupId: Column[String]) = for (
    gr <- groups if (gr.id === groupId)
  ) yield (gr.name, gr.description)

  val queryUpdateGroup = Compiled(queryDefineUpdateGroup _)

  private def queryDefineGetGroup(groupId: Column[String]) = for (
    gr <- groups if (gr.id === groupId)
  ) yield (gr)

  val queryGetGroup = Compiled(queryDefineGetGroup _)

  private def queryDefineCategoryMatching(categoryId: Column[String]) = for (
    cm <- categoryMatching if (cm.category === categoryId)
  ) yield (cm)

  val querysubCategoryMatching = Compiled(queryDefineCategoryMatching _)

  private def queryDefineSubcategoryAssociationRules(categoryId: Column[String]) = for (
    sca <- categoryAssoc if (sca.category === categoryId)
  ) yield (sca.categoryRelated)

  val queryCategoryAssociationRules = Compiled(queryDefineSubcategoryAssociationRules _)

  val queryGetGroupsCategories = Compiled(for (
    (g, c) <- groups leftJoin categories on (_.id === _.group)
  ) yield (g, c.?))

  val queryGetGroupsCategoriesManualLoading = Compiled(for (
    (g, c) <- groups leftJoin (categories.filter(x =>  x.allowManualLoading === true)) on (_.id === _.group)
  ) yield (g, c.?))

  val queryGetMapping = Compiled(for (
    ((c, g), cm) <- (categories.filter(x =>  x.replicate === true && x.pedigreeAssociation === false) innerJoin groups on(_.group === _.id) leftJoin categoryMappingTable on (_._1.id === _.id) ).sortBy(_._1._2.name)
  ) yield (c.id,c.name,g.name, cm.idSuperior.?))

  private def queryGetMappingById(id: Column[String]) = categoryMappingTable.filter(_.id === id)
  val getMappingById = Compiled(queryGetMappingById _)
  private def queryGetMappingReverseById(id: Column[String]) = categoryMappingTable.filter(_.idSuperior === id)
  val getMappingReverseById = Compiled(queryGetMappingReverseById _)
  override def listGroupsAndCategories: Future[Seq[(Group, Option[Category])]] = Future {
    DB.withSession { implicit session =>
      queryGetGroupsCategories.list.map {
        case (group, maybeCat) =>
          val gr = Group(AlphanumericId(group.id), group.name, group.description)
          val cat = maybeCat.map { x => Category(AlphanumericId(x.id), AlphanumericId(group.id), x.name, x.isReference, x.description) }
          (gr, cat)
      }
    }
  }

  override def listGroupsAndCategoriesManualLoading: Future[Seq[(Group, Option[Category])]] = Future {
    DB.withSession { implicit session =>
      queryGetGroupsCategoriesManualLoading.list.map {
        case (group, maybeCat) =>
          val gr = Group(AlphanumericId(group.id), group.name, group.description)
          val cat = maybeCat.map { x => Category(AlphanumericId(x.id), AlphanumericId(group.id), x.name, x.isReference, x.description) }
          (gr, cat)
      }
    }
  }

  override def listCategories: Future[Seq[FullCategory]] = Future {
    DB.withSession { implicit session =>

      val alias = aliasQuery.list

      val assocs = assocQuery.list

      val matchs = matchQuery.list

      val conf = confQuery.list

      categoryQuery.list map { cat =>

        val thisAlias = alias.filter { _.category == cat.id }.map { _.alias }

        val thisAssocs = assocs.filter { _.category == cat.id }.map { x =>
          CategoryAssociation(x.`type`, AlphanumericId(x.categoryRelated), x.mismatchs)
        }

        val thisConf = conf.filter { _.category == cat.id }.map { x =>
          x.`type` -> CategoryConfiguration(x.collectionUri, x.draftUri,
            x.minLocusPerProfile, x.maxOverageDeviatedLoci, x.maxAllelesPerLocus)
        }.toMap

        val thisMatchRules = matchs.filter { _._1.category == cat.id }.map {
          case (cmr, at) => MatchingRule(cmr.`type`, AlphanumericId(cmr.categoryRelated),
            Stringency.withName(cmr.minimumStringency), cmr.failOnMatch.getOrElse(false),
            cmr.forwardToUpper.getOrElse(false), Algorithm.withName(cmr.matchingAlgorithm),
            cmr.minLocusMatch, cmr.mismatchsAllowed, cmr.considerForN, at.mitochondrial)
        }

        FullCategory(
          AlphanumericId(cat.id),
          cat.name,
          cat.description,
          AlphanumericId(cat.group),
          cat.isReference,
          cat.filiationData,
          cat.replicate,
          cat.pedigreeAssociation,
          cat.allowManualLoading,
          thisConf,
          thisAssocs,
          thisAlias,
          thisMatchRules,
          Some(cat.tipo))
      }
    }
  }

  override def addAliases(categoryId: AlphanumericId, alias: Seq[String])(implicit s: Session): Either[String, AlphanumericId] = {

    def addAlias(al: String): Either[String, AlphanumericId] = {
      try {
        categoriesAlias += CategoryAliasRow(al, categoryId.text)
        Right(categoryId)
      } catch {
        case e: SQLException if e.getSQLState.startsWith("23") => Left(Messages("error.E0640", al))
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage)
        }
      }
    }
    alias.foldLeft[Either[String,AlphanumericId]](Right(categoryId)){
      case (prev, current) => prev.fold(Left(_),r=>addAlias(current))
    }
  }

  override def addAssociations(categoryId: AlphanumericId, associations: Seq[CategoryAssociation])(implicit s: Session): Either[String, AlphanumericId] = {
    try {
      val rows = associations.map { assoc =>
        CategoryAssociationRow(0, categoryId.text, assoc.categoryRelated.text, assoc.mismatches, assoc.`type`) }
      categoryAssoc ++= rows
      Right(categoryId)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def addMatchingRules(categoryId: AlphanumericId, matchingRules: Seq[MatchingRule])(implicit s: Session): Either[String, AlphanumericId] = {
    try {
      val rows = matchingRules.map {
        mr =>
          CategoryMatchingRow(0, categoryId.text, mr.categoryRelated.text, 1, mr.minimumStringency.toString,
            Option(mr.failOnMatch), Option(mr.forwardToUpper), mr.matchingAlgorithm.toString,
            mr.minLocusMatch, mr.mismatchsAllowed, mr.`type`, mr.considerForN)
      }

      categoryMatching ++= rows
      Right(categoryId)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def addGroup(group: Group): Future[AlphanumericId] = Future {
    DB.withTransaction { implicit session =>
      groups += GroupRow(group.id.text, group.name, group.description)
      group.id
    }
  }
  override def updateGroup(group: Group): Future[Int] = Future {
    DB.withTransaction { implicit session =>
      queryUpdateGroup(group.id.text).update((group.name, None))
    }
  }

  override def removeGroup(groupId: AlphanumericId): Future[Int] = Future {
    DB.withTransaction { implicit session =>
      queryGetGroup(groupId.text).delete
    }
  }

  override def addCategory(cat: Category): Future[AlphanumericId] = Future {
    DB.withSession {
      implicit session =>
        val subCatRow = CategoryRow(
          cat.id.text,
          cat.group.text,
          cat.name,
          cat.isReference,
          cat.description)
        val res = categories += subCatRow
        cat.id
    }
  }

  override def deleteAliases(categoryId: AlphanumericId)(implicit session: Session): Either[String, AlphanumericId] = {
    try {
      queryGetAlias(categoryId.text).delete
      Right(categoryId)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def deleteAssociations(categoryId: AlphanumericId)(implicit session: Session): Either[String, AlphanumericId] = {
    try {
      queryGetAssocs(categoryId.text).delete
      Right(categoryId)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def deleteMatchingRules(categoryId: AlphanumericId)(implicit session: Session): Either[String, AlphanumericId] = {
    try {
      queryGetMatchRules(categoryId.text).delete
      Right(categoryId)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def deleteMatchingRule(categoryId: AlphanumericId, categoryRelatedId: AlphanumericId)(implicit session: Session): Either[String, AlphanumericId] = {
    try {
      queryGetMatchRule((categoryId.text, categoryRelatedId.text)).delete
      Right(categoryId)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def removeCategory(categoryId: AlphanumericId): Future[Int] = Future {
    DB.withTransaction { implicit session =>
      getMappingById(categoryId.text).delete
      val res = queryGetCategory(categoryId.text).delete
      res
    }
  }

  override def updateCategory(category: Category): Future[Int] = Future {
    DB.withTransaction { implicit session =>
      val res = queryGetCategoryUpd(category.id.text).update((category.name, category.description))
      res
    }
  }

  override def addConfigurations(categoryId: AlphanumericId, configurations: Map[Int, CategoryConfiguration])(implicit session: Session): Either[String, AlphanumericId] = {
    try {
      val rows = configurations.map {
        case (analysisType, conf) =>
          CategoryConfigurationRow(0, categoryId.text, analysisType, conf.collectionUri, conf.draftUri,
            conf.minLocusPerProfile, conf.maxOverageDeviatedLoci, conf.maxAllelesPerLocus) }
      categoryConfiguration ++= rows
      Right(categoryId)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def deleteConfigurations(categoryId: AlphanumericId)(implicit session: Session): Either[String, AlphanumericId] = {
    try {
      queryGetConfigurations(categoryId.text).delete
      Right(categoryId)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def updateFullCategory(cat: FullCategory)(implicit session: Session): Either[String, AlphanumericId] = {
    try {
      val tuple = (cat.name, cat.description, cat.filiationDataRequired,
        cat.replicate, cat.pedigreeAssociation, cat.isReference, cat.manualLoading)
      queryFullCategoryUpd(cat.id.text).update(tuple)
      Right(cat.id)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        Left(e.getMessage)
      }
    }
  }

  override def insertOrUpdateMapping(categoryMappings: CategoryMappingList): Future[Either[String, Unit]] = {

    this.runInTransactionAsync { implicit session => {
      try {
        for (categoryMapping <- categoryMappings.categoryMappingList) {
          val id = (categoryMappingTable returning categoryMappingTable.map(_.id)) insertOrUpdate CategoryMappingRow(categoryMapping.id.text, categoryMapping.idSuperior)
        }
        Right(())
      } catch {
        case e: Exception => {
          e.printStackTrace()
          Left(e.getMessage)
        }
      }
    }
    }
  }

  override def listCategoriesMapping: Future[List[FullCategoryMapping]] = {

    this.runInTransactionAsync { implicit session => {
      queryGetMapping.list.map( x => FullCategoryMapping(AlphanumericId(x._1),x._2,x._3,x._4.getOrElse("")))
    }

    }
  }

  def deleteCategoryMappingById(id: String): Future[Either[String, String]] = {
    this.runInTransactionAsync { implicit session => {
      try {
        getMappingById(id).delete
        Right(id)
      } catch {
        case e: Exception => {
          Left(e.getMessage)
        }
      }
    }
    }
  }

  override def getCategoriesMappingById(id:AlphanumericId): Future[Option[String]] = {

    this.runInTransactionAsync { implicit session => {
      val first = getMappingById(id.text).firstOption

      first match {
        case None => None
        case Some(Tables.CategoryMappingRow(_,"")) =>None
        case Some(Tables.CategoryMappingRow(_,categorySup)) => Some(categorySup)
      }

    }

    }
  }
  override def getCategoriesMappingReverseById(id:AlphanumericId): Future[Option[AlphanumericId]] = {

    this.runInTransactionAsync { implicit session => {
      getMappingReverseById(id.text).firstOption.map(_.id).map(AlphanumericId(_))
      }
    }
  }
}
