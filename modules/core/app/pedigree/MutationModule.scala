package pedigree

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.{Configuration, Environment}

class MutationModule(env: Environment, conf: Configuration) extends AbstractModule:
  override def configure(): Unit =
    val defaultMutationRateI            = conf.get[String]("mt.defaultMutationRateI")
    val defaultMutationRateF            = conf.get[String]("mt.defaultMutationRateF")
    val defaultMutationRateM            = conf.get[String]("mt.defaultMutationRateM")
    val defaultMutationRange            = conf.get[String]("mt.defaultMutationRange")
    val defaultMutationRateMicrovariant = conf.get[String]("mt.defaultMutationRateMicrovariant")

    bind(classOf[String]).annotatedWith(Names.named("defaultMutationRateI"))
      .toInstance(defaultMutationRateI)
    bind(classOf[String]).annotatedWith(Names.named("defaultMutationRateF"))
      .toInstance(defaultMutationRateF)
    bind(classOf[String]).annotatedWith(Names.named("defaultMutationRateM"))
      .toInstance(defaultMutationRateM)
    bind(classOf[String]).annotatedWith(Names.named("defaultMutationRange"))
      .toInstance(defaultMutationRange)
    bind(classOf[String]).annotatedWith(Names.named("defaultMutationRateMicrovariant"))
      .toInstance(defaultMutationRateMicrovariant)

    bind(classOf[MutationRepository]).to(classOf[SlickMutationRepository])
    bind(classOf[MutationService]).to(classOf[MutationServiceImpl])
    ()
