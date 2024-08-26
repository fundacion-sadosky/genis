package stats

import com.google.inject.AbstractModule

class StatsModule extends AbstractModule {
  override protected def configure() {
    bind(classOf[PopulationBaseFrequencyRepository]).to(classOf[SlickPopulationBaseFrequencyRepository])
    bind(classOf[PopulationBaseFrequencyService]).to(classOf[PopulationBaseFrequencyImpl])
    ()
  }
}