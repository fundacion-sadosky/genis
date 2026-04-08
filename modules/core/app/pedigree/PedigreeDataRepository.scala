package pedigree

import jakarta.inject.Singleton

import scala.concurrent.Future

// Minimal types needed by TraceService.getFullDescription for PedigreeMatchActionInfo.
// TODO: migrate pedigree — replace these stubs with the full PedigreeDataRepository migration.
case class PedigreeMetaData(id: Long, name: String, assignee: String)
case class PedigreeDataCreation(pedigreeMetaData: PedigreeMetaData)

trait PedigreeDataRepository {
  def getPedigreeMetaData(pedigreeId: Long): Future[Option[PedigreeDataCreation]]
}

@Singleton
class PedigreeDataRepositoryStub extends PedigreeDataRepository {
  // TODO: migrate pedigree
  override def getPedigreeMetaData(pedigreeId: Long): Future[Option[PedigreeDataCreation]] =
    Future.successful(None)
}
