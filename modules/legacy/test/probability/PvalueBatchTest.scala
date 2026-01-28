package probability

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import profile.Profile
import specs.PdgSpec
import stubs.Stubs
import profile.MongoProfileRepository
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import profile.Allele
import probability.PValueCalculator._
import org.scalatest.Ignore
import java.io.File
import java.io.PrintWriter
import play.api.libs.json.Json
import types.SampleCode
import scala.io.Source

@Ignore
class PvalueBatchTest extends PdgSpec {

  val duration = Duration(10, SECONDS)

  val frequencyTable = {

    val pbf = Stubs.populationBaseFrequency

    val list = pbf.base map { sample => ((sample.marker, BigDecimal(sample.allele)), sample.frequency.toDouble) }

    list.toMap[(String, BigDecimal), Double]
  }

  /*"PvalueBatchTest" must {
    "generates batch" in {

      val writer = new PrintWriter(new File("/tmp/output.txt"))

      val pvalCalc = autosomalRMP(frequencyTable)(new HardyWeinbergCalculationProbability) _

      val profileRespository = new MongoProfileRepository
      val profileFuture = profileRespository.findAll.map { profiles: List[Profile] =>
        profiles.map { profile =>
          profile.genotypification.map { g =>
            writer.write(g._1)
            writer.write(":")
            writer.write("[" + g._2.map { a =>
              a match {
                case Allele(x) => x.toString
                case Dropout   => "*"
              }
            }.mkString(",") + "]")
            writer.write(";")
          }
          val p = pvalCalc(profile.genotypification.map { l => l._1 }.toSet, profile)
          writer.write("PVALUE:")
          writer.write(p.toString())
          writer.write(";" + profile.globalCode)
          writer.write("\n")
        }
      }
      val resultOfEnumerate = Await.result(profileFuture, Duration(200, SECONDS))
      writer.close()

    }
  }

  "PvalueBatchTest" must {
    "calculate one" in {

      val pvalCalc = autosomalRMP(frequencyTable)(new HardyWeinbergCalculationProbability) _

      val profileRespository = new MongoProfileRepository
      val profileFuture = profileRespository.findByCode(SampleCode("AR-C-SHDG-290")).map { profiles =>
        profiles.map { profile =>
          val p = pvalCalc(profile.genotypification.map { l => l._1 }.toSet, profile)
          println(profile.globalCode + " was " + p)
        }
      }
      val resultOfEnumerate = Await.result(profileFuture, Duration(100, SECONDS))

    }
  }*/

}