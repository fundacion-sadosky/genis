package matching

import java.util.Date

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.scheduler.JobSucceeded
import org.apache.spark.scheduler.SparkListener
import org.apache.spark.scheduler.SparkListenerJobEnd
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.bson.BsonDocument
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.Document
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistries
import com.mongodb.DBObject
import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
import com.mongodb.ServerAddress
import com.mongodb.spark.MongoClientFactory
import com.mongodb.spark.MongoConnector
import play.api.Logger
import types.AlphanumericId
import types.MongoDate
import types.MongoId
import types.SampleCode

class MatchingJobsSparkListener extends SparkListener() {

  private val logger = Logger(this.getClass)

  override def onJobEnd(jobEnd: SparkListenerJobEnd) = {
    val result = jobEnd.jobResult

    result match {
      case JobSucceeded => logger.trace(s"JobId:${jobEnd.jobId} Ended OK")
      case _ => {
        logger.error(s"JobId:${jobEnd.jobId} Ended Bad $result")
      }
    }
  }
}

class MatchResultCodec extends Codec[MatchResult] {
  def decode(reader: BsonReader, ctx: DecoderContext): matching.MatchResult = {
    MatchResult(
      MongoId("AAAA"),
      MongoDate(new Date()),
      1,
      MatchingProfile(SampleCode("AR-B-IACA-501"), "sura", MatchStatus.hit, None, AlphanumericId("Victimas")),
      MatchingProfile(SampleCode("AR-B-IACA-501"), "sura", MatchStatus.hit, None, AlphanumericId("Condenados") ),
      NewMatchingResult(
        Stringency.HighStringency,
        Map.empty,
        10,
        AlphanumericId("XXXX"),
        0.5,
        0.5,
        Algorithm.ENFSI),
      1,
      None,
      None,
      0.0,
//      0.0,
      0)
  }

  def encode(writer: BsonWriter, obj: matching.MatchResult, ctx: EncoderContext): Unit = {
  }

  def getEncoderClass(): Class[matching.MatchResult] = {
    classOf[MatchResult]
  }

}

class CustomMongoClientFactory(sparkConf: SparkConf) extends MongoClientFactory {

  def create(): MongoClient = {
    val defaultCodecs = MongoClient.getDefaultCodecRegistry
    val customCodecs = CodecRegistries.fromCodecs(new MatchResultCodec())
    val codecs = CodecRegistries.fromRegistries(customCodecs, defaultCodecs)

    val options = MongoClientOptions
      .builder()
      .codecRegistry(codecs)
      .build()
    new MongoClient(new ServerAddress("localhost"), options)
  }
}

object Spark2 {

  private val logger = Logger(this.getClass)

  lazy val sparkSession = SparkSession.builder()
    .master("local[*]")
    .appName("Genis Matching Service")
    .config("spark.app.id", "Genis Matching Service")
    //.config("spark.rdd.compress", true)
    //.config("spark.driver.cores", 2)
    //.config("spark.broadcast.blockSize", "1m")
    .getOrCreate()

  lazy val context: SparkContext = {
    val sc = sparkSession.sparkContext
    sc.addSparkListener(new MatchingJobsSparkListener())
    sc
  }

  lazy val connector = MongoConnector(new CustomMongoClientFactory(context.getConf))
}