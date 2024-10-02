package laboratories

import javax.inject.{Inject, Singleton}

import models.Tables
import models.Tables.GeneticistRow
import play.api.Application
import play.api.db.slick.Config.driver.simple.{Column, Compiled, TableQuery, columnExtensionMethods, longColumnType, queryToInsertInvoker, runnableCompiledToAppliedQueryInvoker, slickDriver, stringColumnType, valueToConstColumn}
import play.api.db.slick.DB
import types.Email
import util.DefaultDb

import scala.concurrent.Future
import slick.driver.PostgresDriver.simple._

abstract class GeneticistRepository {

  def add(geneticist: Geneticist): Future[Int]
  def getAll(laboratory: String): Future[Seq[Geneticist]]
  def update(geneticist: Geneticist): Future[Int]
  def get(id: Long): Future[Option[Geneticist]]
}


@Singleton
class SlickGeneticistRepository @Inject() (implicit app: Application) extends GeneticistRepository with DefaultDb {
  
  val geneticists: TableQuery[Tables.Geneticist] = Tables.Geneticist
  
  private def queryDefineGetAllByLab(lab: Column[String]) = for (
      gen <- geneticists if gen.laboratory === lab ) yield (gen)
  
  val queryGetAllByLab = Compiled(queryDefineGetAllByLab _)  
  
  private def queryDefindeGetGeneticist(id: Column[Long]) = for (
      gen <- geneticists if gen.id === id ) yield (gen)

  val queryGetGeneticist = Compiled(queryDefindeGetGeneticist _)
      
  private def queryDefineUpdateGeneticist(id: Column[Long]) = for (
    gen <- geneticists if gen.id === id ) yield (gen.name,gen.lastname,gen.email,gen.telephone)    
  
  val queryUpdateGeneticist = Compiled(queryDefineUpdateGeneticist _)
  
  override def get(id: Long): Future[Option[Geneticist]] = Future {
    DB.withSession { implicit session =>  
      
      queryGetGeneticist(id).firstOption map { gen =>
        Geneticist(gen.name,gen.laboratory,gen.lastname,Email(gen.email),gen.telephone,Option(gen.id))
      }
    }
  }
  
  override def update(geneticist: Geneticist): Future[Int] = Future {
    
   DB.withTransaction { implicit session =>  
       
     val q = for (gen <- geneticists if gen.id === geneticist.id ) 
         yield (gen.name,gen.lastname,gen.email,gen.telephone,gen.laboratory)
   
     val arg = (geneticist.name,geneticist.lastname,geneticist.email.text,geneticist.telephone,geneticist.laboratory)
         
    q.update(arg)    
  }}
  
  override def add(geneticist: Geneticist): Future[Int] = Future {
    
    DB.withTransaction{implicit session => 
    	val gen = new GeneticistRow(0,geneticist.laboratory,geneticist.name,geneticist.lastname,geneticist.email.text,geneticist.telephone)
    	geneticists += gen
    }
  }
  
  override def getAll(laboratory: String): Future[Seq[Geneticist]] = Future {
    
    DB.withSession{implicit session => 
    	queryGetAllByLab(laboratory).list map {gen => new Geneticist(gen.name,gen.laboratory,gen.lastname,Email(gen.email),gen.telephone,Option(gen.id)) }	
    }
  }
}