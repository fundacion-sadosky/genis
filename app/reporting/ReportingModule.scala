package reporting

import com.google.inject.{AbstractModule, Provides}

/**
  * Created by pdg on 3/1/18.
  */
class ReportingModule () extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ReportingService]).to(classOf[ReportingServiceImpl])
    bind(classOf[ProfileReportService]).to(classOf[ProfileReportServiceImpl])
    bind(classOf[ProfileReportMongoRepository]).to(classOf[MongoProfileReportRepository])
    bind(classOf[ProfileReportPostgresRepository]).to(classOf[PostgresProfileReportRepository])
  }

  /**
    * Provides PDF generator implementation.
    *
    * @return PDF generator implementation.
    */
  @Provides
  def providePdfGenerator(): PdfGenerator = {
    val pdfGen = new PdfGenerator()
    pdfGen.loadLocalFonts(Seq("app/assets/stylesheets/report/DejaVuSans.ttf", "app/assets/stylesheets/report/DejaVuSans-Bold.ttf"))
    pdfGen
  }

}
