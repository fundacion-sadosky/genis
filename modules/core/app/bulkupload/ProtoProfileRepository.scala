package bulkupload

import configdata.MatchingRule
import jakarta.inject.Singleton
import kits.StrKitLocus
import profile.Profile
import search.PaginationSearch
import types.{AlphanumericId, SampleCode}

import java.sql.Timestamp
import scala.concurrent.Future

// TODO: migrate ProtoProfileRepository — full Slick 3.5 implementation in Layer 5
trait ProtoProfileRepository {
  def createBatch(user: String, protoProfileStream: LazyList[ProtoProfile], laboratory: String, kits: Map[String, List[StrKitLocus]], label: Option[String], analysisType: String): Future[Long]
  def getBatch(batchId: Long): Future[Option[ProtoProfilesBatch]]
  def getBatchesStep1(userId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]]
  def countBatchesStep1(userId: String, isSuperUser: Boolean): Future[Int]
  def getBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]]
  def countBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean): Future[Int]
  def getProtoProfilesStep1(batchId: Long, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]]
  def getProtoProfilesStep2(batchId: Long, userId: String, isSuperUser: Boolean, paginationSearch: Option[PaginationSearch] = None): Future[Seq[ProtoProfile]]
  def getProtoProfile(id: Long): Future[Option[ProtoProfile]]
  def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]]
  def updateProtoProfileStatus(id: Long, status: ProtoProfileStatus.Value): Future[Int]
  def updateProtoProfileData(id: Long, cat: AlphanumericId): Future[Int]
  def updateProtoProfileMatchingRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Int]
  def exists(sampleName: String): Future[(Option[SampleCode], Option[Long])]
  def validateAssigneAndCategory(globalCode: SampleCode, assigne: String, category: Option[AlphanumericId]): Future[Option[String]]
  def hasProfileDataFiliation(id: Long): Future[Boolean]
  def setRejectMotive(id: Long, motive: String, rejectionUser: String, idMotive: Long, timeRejected: Timestamp): Future[Int]
  def canDeleteKit(id: String): Future[Boolean]
  def deleteBatch(id: Long): Future[Either[String, Long]]
  def countImportedProfilesByBatch(idBatch: Long): Future[Either[String, Long]]
  def countAllProtoProfilesInBatch(batchId: Long): Future[Int]
  def getSearchBachLabelID(userId: String, isSuperUser: Boolean, filter: String): Future[Seq[ProtoProfilesBatchView]]
  def getBatchSearchModalViewByIdOrLabel(input: String, idCase: Long): Future[List[BatchModelView]]
  def mtExistente(sampleName: String): Future[Boolean]
  def updateProtoProfileStatus(internalCode: String, status: String): Future[Int]
  def getProtoProfileStatus(internalCode: String): Future[String]
}

@Singleton
class ProtoProfileRepositoryStub extends ProtoProfileRepository {
  override def createBatch(user: String, protoProfileStream: LazyList[ProtoProfile], laboratory: String, kits: Map[String, List[StrKitLocus]], label: Option[String], analysisType: String): Future[Long] = Future.successful(0L)
  override def getBatch(batchId: Long): Future[Option[ProtoProfilesBatch]] = Future.successful(None)
  override def getBatchesStep1(userId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]] = Future.successful(Nil)
  override def countBatchesStep1(userId: String, isSuperUser: Boolean): Future[Int] = Future.successful(0)
  override def getBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean, offset: Int, limit: Int): Future[Seq[ProtoProfilesBatchView]] = Future.successful(Nil)
  override def countBatchesStep2(userId: String, geneMapperId: String, isSuperUser: Boolean): Future[Int] = Future.successful(0)
  override def getProtoProfilesStep1(batchId: Long, paginationSearch: Option[PaginationSearch]): Future[Seq[ProtoProfile]] = Future.successful(Nil)
  override def getProtoProfilesStep2(batchId: Long, userId: String, isSuperUser: Boolean, paginationSearch: Option[PaginationSearch]): Future[Seq[ProtoProfile]] = Future.successful(Nil)
  override def getProtoProfile(id: Long): Future[Option[ProtoProfile]] = Future.successful(None)
  override def getProtoProfileWithBatchId(id: Long): Future[Option[(ProtoProfile, Long)]] = Future.successful(None)
  override def updateProtoProfileStatus(id: Long, status: ProtoProfileStatus.Value): Future[Int] = Future.successful(0)
  override def updateProtoProfileData(id: Long, cat: AlphanumericId): Future[Int] = Future.successful(0)
  override def updateProtoProfileMatchingRulesMismatch(id: Long, matchingRules: Seq[MatchingRule], mismatches: Profile.Mismatch): Future[Int] = Future.successful(0)
  override def exists(sampleName: String): Future[(Option[SampleCode], Option[Long])] = Future.successful((None, None))
  override def validateAssigneAndCategory(globalCode: SampleCode, assigne: String, category: Option[AlphanumericId]): Future[Option[String]] = Future.successful(None)
  override def hasProfileDataFiliation(id: Long): Future[Boolean] = Future.successful(false)
  override def setRejectMotive(id: Long, motive: String, rejectionUser: String, idMotive: Long, timeRejected: Timestamp): Future[Int] = Future.successful(0)
  override def canDeleteKit(id: String): Future[Boolean] = Future.successful(false)
  override def deleteBatch(id: Long): Future[Either[String, Long]] = Future.successful(Right(id))
  override def countImportedProfilesByBatch(idBatch: Long): Future[Either[String, Long]] = Future.successful(Right(0L))
  override def countAllProtoProfilesInBatch(batchId: Long): Future[Int] = Future.successful(0)
  override def getSearchBachLabelID(userId: String, isSuperUser: Boolean, filter: String): Future[Seq[ProtoProfilesBatchView]] = Future.successful(Nil)
  override def getBatchSearchModalViewByIdOrLabel(input: String, idCase: Long): Future[List[BatchModelView]] = Future.successful(List.empty)
  override def mtExistente(sampleName: String): Future[Boolean] = Future.successful(false)
  override def updateProtoProfileStatus(internalCode: String, status: String): Future[Int] = Future.successful(0)
  override def getProtoProfileStatus(internalCode: String): Future[String] = Future.successful("")
}
