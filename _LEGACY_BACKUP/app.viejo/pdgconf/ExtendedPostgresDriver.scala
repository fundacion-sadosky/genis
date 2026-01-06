package pdgconf

import scala.slick.driver.PostgresDriver

import com.github.tminglei.slickpg.PgArraySupport
import com.github.tminglei.slickpg.PgDateSupport
import com.github.tminglei.slickpg.PgHStoreSupport
import com.github.tminglei.slickpg.PgPlayJsonSupport
import com.github.tminglei.slickpg.PgPostGISSupport
import com.github.tminglei.slickpg.PgRangeSupport
import com.github.tminglei.slickpg.PgSearchSupport

trait ExtendedPostgresDriver extends PostgresDriver
    with PgArraySupport
    with PgDateSupport
    with PgRangeSupport
    with PgHStoreSupport
    with PgPlayJsonSupport
    with PgSearchSupport {
  override val pgjson = "jsonb" //to keep back compatibility, pgjson's value was "json" by default

  override lazy val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  //////
  trait ImplicitsPlus extends Implicits
    with ArrayImplicits
    with DateTimeImplicits
    with RangeImplicits
    with HStoreImplicits
    with JsonImplicits
    with SearchImplicits

  trait SimpleQLPlus extends SimpleQL
    with ImplicitsPlus
    with SearchAssistants

}

object ExtendedPostgresDriver extends ExtendedPostgresDriver

