package stats

import models.Tables
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick._
import probability.ProbabilityModel
import specs.PdgSpec
import stubs.Stubs
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import slick.lifted.TableQuery

class PopulationBaseFrequencyRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)

  val frequencies: TableQuery[Tables.PopulationBaseFrequency] = Tables.PopulationBaseFrequency
  val baseFrequencies: TableQuery[Tables.PopulationBaseFrequencyName] = Tables.PopulationBaseFrequencyName

  private def queryDefineGetBaseFrequencyName(name: Column[String]) = Compiled(for (
    frequency <- baseFrequencies if frequency.name === name
  ) yield frequency)

  private def queryDefineGetBaseFrequency(id: Column[Long]) = Compiled(for (
    frequency <- frequencies if frequency.baseName === id
  ) yield frequency)

  "A PopulationBaseFrequencyRepository" must{
    "save a PopulationBaseFrequency in the db" in{
      val popBaseFreqRepo = new SlickPopulationBaseFrequencyRepository

      val seqPopulation: Seq[PopulationSampleFrequency] = List(
        PopulationSampleFrequency("TPOX", 5, BigDecimal("0.00016000")),
        PopulationSampleFrequency("TPOX", 6, BigDecimal("0.00261000")),
        PopulationSampleFrequency("TPOX", 7, BigDecimal("0.00142000")))

      val populationBaseFrequency = new PopulationBaseFrequency("pop freq 2", 0, ProbabilityModel.HardyWeinberg, seqPopulation)
      
      val inserts = Await.result(popBaseFreqRepo.add(populationBaseFrequency),duration)
      inserts.get mustBe populationBaseFrequency.base.size

      DB.withSession { implicit session =>
        val frequency = queryDefineGetBaseFrequencyName("pop freq 2")
        val id = frequency.first.id
        queryDefineGetBaseFrequency(id).delete
        frequency.delete
      }
    }
  }
  
  "A PopulationBaseFrequencyRepository" must{
    "get all PopulationBaseFrequency names from the db" in {
      val popBaseFreqRepo = new SlickPopulationBaseFrequencyRepository

      val seqPopulation: Seq[PopulationSampleFrequency] = List(
        PopulationSampleFrequency("TPOX", 5, BigDecimal("0.00016000")),
        PopulationSampleFrequency("TPOX", 6, BigDecimal("0.00261000")),
        PopulationSampleFrequency("TPOX", 7, BigDecimal("0.00142000")))

      val populationBaseFrequency = new PopulationBaseFrequency("pop freq 2", 0, ProbabilityModel.HardyWeinberg, seqPopulation)

      Await.result(popBaseFreqRepo.add(populationBaseFrequency), duration)

      val popBaseFreq = Stubs.populationBaseFrequency

      val allNames = Await.result(popBaseFreqRepo.getAllNames, duration)
      allNames.size mustBe 1

      DB.withSession { implicit session =>
        val frequency = queryDefineGetBaseFrequencyName("pop freq 2")
        val id = frequency.first.id
        queryDefineGetBaseFrequency(id).delete
        frequency.delete
      }
    }
  }
  
  "A PopulationBaseFrequencyRepository" must{
    "retrieve a PopulationBaseFrequency by name" in{
      val popBaseFreqRepo = new SlickPopulationBaseFrequencyRepository

      val seqPopulation: Seq[PopulationSampleFrequency] = List(
        PopulationSampleFrequency("TPOX", 5, BigDecimal("0.00016000")),
        PopulationSampleFrequency("TPOX", 6, BigDecimal("0.00261000")),
        PopulationSampleFrequency("TPOX", 7, BigDecimal("0.00142000")))

      val populationBaseFrequency = new PopulationBaseFrequency("pop freq 2", 0, ProbabilityModel.HardyWeinberg, seqPopulation)

      Await.result(popBaseFreqRepo.add(populationBaseFrequency),duration)

      val popBaseFreq = Await.result(popBaseFreqRepo.getByName("pop freq 2"), duration)

      popBaseFreq.get mustBe populationBaseFrequency

      DB.withSession { implicit session =>
        val frequency = queryDefineGetBaseFrequencyName("pop freq 2")
        val id = frequency.first.id
        queryDefineGetBaseFrequency(id).delete
        frequency.delete
      }
    }
  }
  
  "A PopulationBaseFrequencyRepository" must{
   "set a default to a base" in{
     val popBaseFreqRepo = new SlickPopulationBaseFrequencyRepository

     val seqPopulation: Seq[PopulationSampleFrequency] = List(
       PopulationSampleFrequency("TPOX", 5, BigDecimal("0.00016000")),
       PopulationSampleFrequency("TPOX", 6, BigDecimal("0.00261000")),
       PopulationSampleFrequency("TPOX", 7, BigDecimal("0.00142000")))

     val populationBaseFrequency = new PopulationBaseFrequency("pop freq 2", 0, ProbabilityModel.HardyWeinberg, seqPopulation)

     Await.result(popBaseFreqRepo.add(populationBaseFrequency),duration)
     
     val sets = Await.result(popBaseFreqRepo.setAsDefault(populationBaseFrequency.name),duration)
     sets mustBe 1

     DB.withSession { implicit session =>
       val frequency = queryDefineGetBaseFrequencyName("pop freq 2")
       val id = frequency.first.id
       queryDefineGetBaseFrequency(id).delete
       frequency.delete
     }
   }  
  }
  
  "A PopulationBaseFrequencyRepository" must{
    "deactivate a PopulationBaseFrequency in the db" in{
      val popBaseFreqRepo = new SlickPopulationBaseFrequencyRepository

      val seqPopulation: Seq[PopulationSampleFrequency] = List(
        PopulationSampleFrequency("TPOX", 5, BigDecimal("0.00016000")),
        PopulationSampleFrequency("TPOX", 6, BigDecimal("0.00261000")),
        PopulationSampleFrequency("TPOX", 7, BigDecimal("0.00142000")))

      val populationBaseFrequency = new PopulationBaseFrequency("pop freq 2", 0, ProbabilityModel.HardyWeinberg, seqPopulation)

      Await.result(popBaseFreqRepo.add(populationBaseFrequency),duration)
      
      val deletes = Await.result(popBaseFreqRepo.toggleStatePopulationBaseFrequency(populationBaseFrequency.name),duration).get
      deletes mustBe 1

      Await.result(popBaseFreqRepo.toggleStatePopulationBaseFrequency(populationBaseFrequency.name),duration).get

      DB.withSession { implicit session =>
        val frequency = queryDefineGetBaseFrequencyName("pop freq 2")
        val id = frequency.first.id
        queryDefineGetBaseFrequency(id).delete
        frequency.delete
      }
    }
  } 
}