package matching

import java.util.Date
import javax.inject.{Inject, Named, Singleton}

import com.mongodb.client.{MongoCollection, MongoDatabase}
import com.mongodb.client.model.{Aggregates, Filters, Projections}
import org.bson.Document
import play.api.Logger
import play.api.libs.json.{Json, JsValue}

import configdata.{CategoryService, MtConfiguration}
import inbox.{MatchingInfo, NotificationService}
import kits.LocusService
import profile.{Profile, ProfileRepository, MtRCRS}
import profiledata.{ProfileDataRepository, ProfileDataService}
import trace.{Trace, TraceService}
import types.{AlphanumericId, SampleCode}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

/** Reemplaza Spark2Matcher: corre el algoritmo de matching en Scala puro usando el
 *  MongoDB sync driver. Sin Spark ni Actor distribuido — todo en un Future del EC.
 *
 *  Cambios estructurales vs legacy:
 *  - MongoSpark RDD → colección sync (`.find(filter).into(list)`)
 *  - ActorSystem MatchingActor → `Future { findMatchesBlocking(...) }`
 *  - `Await.result` para servicios async (igual que legacy usaba Await)
 */
@Singleton
class ProfileMatcher @Inject()(
  database: MongoDatabase,
  profileRepo: ProfileRepository,
  categoryService: CategoryService,
  locusService: LocusService,
  notificationService: NotificationService,
  matchingRepo: MongoMatchingRepository,
  matchStatusService: MatchingProcessStatus,
  profileDataService: ProfileDataService,
  @Named("mtConfig") mtConfig: MtConfiguration,
  @Named("labCode")  currentInstanceLabCode: String
)(using ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  private val awaitDuration: Duration = 100.seconds

  def findMatchesInBackground(globalCode: SampleCode): Unit = {
    Future {
      try findMatchesBlocking(globalCode)
      catch {
        case ex: Exception =>
          logger.error(s"Error running matching for $globalCode", ex)
          matchStatusService.pushJobStatus(MatchJobFail)
      }
    }
    ()
  }

  def findMatchesBlocking(globalCode: SampleCode): Unit = {
    // 1. Loci → locusRangeMap
    val loci          = Await.result(locusService.list(), awaitDuration)
    val locusRangeMap = loci.map(l => l.id -> AleleRange(
      l.minAlleleValue.getOrElse(BigDecimal(0)),
      l.maxAlleleValue.getOrElse(BigDecimal(99))
    )).toMap

    // 2. Perfil buscado
    val profileOpt = Await.result(profileRepo.findByCode(globalCode), awaitDuration)
    profileOpt match {
      case None =>
        logger.debug(s"Profile $globalCode not found, skipping")

      case Some(searched) =>

      // 3. Reglas de matching
      val rules = getMatchingRules(searched)
      if (rules.isEmpty) {
        logger.debug(s"No matching rules for $globalCode, skipping")
      } else {

      logger.debug(s"Start matching process for profile ${searched.globalCode}")
      matchStatusService.pushJobStatus(MatchJobStarted)

      // 4. Perfiles candidatos desde MongoDB
      val categories = rules.map(_.categoryRelated).distinct
      val candidateProfiles = loadCandidateProfiles(globalCode, categories)

      // 5. N: perfiles que cuentan para N-statistic
      val n = candidateProfiles.count { p =>
        rules.exists(r =>
          p.genotypification.keySet.contains(r.`type`) &&
          r.categoryRelated == p.categoryId &&
          r.considerForN
        )
      }.toLong

      // 6. mtRcrs
      val mtRcrs = Await.result(profileDataService.getMtRcrs(), awaitDuration)

      // 7. Aplicar algoritmo de matching
      val newMatches = candidateProfiles.flatMap { candidate =>
        rules
          .filter(_.categoryRelated == candidate.categoryId)
          .map(_.`type`)
          .flatMap { at =>
            val rulesForAt = rules.filter(r => r.`type` == at && r.categoryRelated == candidate.categoryId)
            val enfsi      = rulesForAt.find(_.matchingAlgorithm == Algorithm.ENFSI)
            val mixmix     = rulesForAt.find(_.matchingAlgorithm == Algorithm.GENIS_MM)
            val pIsMix     = searched.contributors.getOrElse(1) == 2
            val qIsMix     = candidate.contributors.getOrElse(1) == 2
            val rule       = if (pIsMix && qIsMix && mixmix.isDefined) mixmix.orElse(enfsi) else enfsi
            rule.flatMap(r => MatchingAlgorithm.performMatch(mtConfig, searched, candidate, r, mtRcrs, None, n, locusRangeMap))
          }
      }

      // 8. Matches existentes (para detectar insert/replace/delete)
      val existingMatches = matchingRepo.getExistingMatches(globalCode)

      // Clasificar
      val matchesToInsert = newMatches.filter { mr =>
        !existingMatches.contains((mr.rightProfile.globalCode.text, mr.`type`))
      }
      val matchesToReplace = newMatches.flatMap { mr =>
        existingMatches.get((mr.rightProfile.globalCode.text, mr.`type`)).map { existingId =>
          // Preservar el _id existente
          val updatedId = MongoId(existingId)
          updatedId -> mr.copy(_id = updatedId)
        }
      }
      val replacedIds     = matchesToReplace.map(_._1.id).toSet
      val matchesToDelete = existingMatches.values.filterNot(replacedIds.contains).toSeq

      logger.info(
        s"Profile ${searched.globalCode}: ${newMatches.size} matches against ${candidateProfiles.size} candidates " +
        s"(insert=${matchesToInsert.size}, replace=${matchesToReplace.size}, delete=${matchesToDelete.size})"
      )

      // 9. Persistir
      matchesToDelete.foreach(id => matchingRepo.markMatchesDeleted(Seq(id)))
      matchesToReplace.foreach { case (_, mr) => matchingRepo.upsertMatch(mr) }
      matchesToInsert.foreach(mr => matchingRepo.upsertMatch(mr))

      // 10. Notificaciones (solo en inserts y replaces)
      val labActual = s"-$currentInstanceLabCode-"
      val allToNotify = matchesToInsert.map(mr => (mr._id.id, mr.leftProfile.globalCode, mr.rightProfile.globalCode,
        mr.leftProfile.assignee, mr.rightProfile.assignee)) ++
        matchesToReplace.map { case (_, mr) => (mr._id.id, mr.leftProfile.globalCode, mr.rightProfile.globalCode,
          mr.leftProfile.assignee, mr.rightProfile.assignee) }

      allToNotify.foreach { case (matchId, leftGc, rightGc, leftAssignee, rightAssignee) =>
        if (!leftGc.text.contains(labActual) && !rightGc.text.contains(labActual)) {
          // Match externo — no notificar localmente
        } else {
          notificationService.push(leftAssignee, MatchingInfo(leftGc, rightGc, matchId, isDesktop = false))
          if (leftAssignee != rightAssignee)
            notificationService.push(rightAssignee, MatchingInfo(rightGc, leftGc, matchId, isDesktop = false))
        }
      }

      // 11. Marcar como procesado (si la categoría no está asociada a pedigree)
      val categoryOpt = Await.result(categoryService.getCategory(searched.categoryId), awaitDuration)
      val isPedigreeAssociated = categoryOpt.exists(_.pedigreeAssociation)
      if (!isPedigreeAssociated) {
        Await.result(profileRepo.setMatcheableAndProcessed(globalCode), awaitDuration)
      }

      matchStatusService.pushJobStatus(MatchJobEndend)
      } // end else (rules.nonEmpty)
    } // end match
  }

  // ── helpers ─────────────────────────────────────────────────────────────────

  private def loadCandidateProfiles(
    excludedCode: SampleCode,
    categories: Seq[AlphanumericId]
  ): Seq[Profile] = {
    val profilesColl: MongoCollection[Document] = database.getCollection("profiles")
    val filter = Filters.and(
      Filters.eq("matcheable", true),
      Filters.eq("deleted", false),
      Filters.ne("globalCode", excludedCode.text),
      Filters.in("categoryId", categories.map(_.text).asJava)
    )
    profilesColl.find(filter)
      .into(new java.util.ArrayList[Document]())
      .asScala
      .flatMap { doc =>
        scala.util.Try(Json.parse(doc.toJson()).as[Profile]).toOption
      }
      .toSeq
  }

  private def getMatchingRules(profile: Profile): Seq[configdata.MatchingRule] = {
    val forense = 1
    profile.matchingRules match {
      case Some(rules) if rules.nonEmpty => rules
      case _ =>
        val categoriesMap = Await.result(categoryService.listCategories, awaitDuration)
        categoriesMap.get(profile.categoryId) match {
          case Some(category) if category.tipo.contains(forense) && !category.pedigreeAssociation =>
            category.matchingRules.filter { r =>
              categoriesMap.get(r.categoryRelated).exists(_.tipo.contains(forense))
            }
          case _ => Nil
        }
    }
  }
}