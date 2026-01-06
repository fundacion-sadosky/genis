package modules

import play.api.{Configuration, Environment}
import play.api.inject.Module

/**
 * Módulo de inyección de dependencias para GENis 6.0
 * Configura todos los servicios, repositorios y filtros
 */
class ApplicationModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq()
}

