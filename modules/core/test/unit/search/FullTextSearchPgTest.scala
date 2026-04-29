package search

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import slick.jdbc.PostgresProfile.api.Database

import scala.concurrent.ExecutionContext

/** Tests de caja blanca para los métodos puros de FullTextSearchPg.
 *  Los métodos son `private[search]` → accesibles desde este paquete.
 *  Se pasa null para la DB porque los métodos no emiten queries.
 */
class FullTextSearchPgTest extends AnyWordSpec with Matchers:

  private given ExecutionContext = ExecutionContext.global
  private val fts = new FullTextSearchPg(null.asInstanceOf[Database])

  // ─── replaceCharacters ────────────────────────────────────────────────────

  "replaceCharacters" must {

    "return null for null input" in {
      fts.replaceCharacters(null) mustBe null
    }

    "return empty string unchanged" in {
      fts.replaceCharacters("") mustBe ""
    }

    "replace LF (\\n) with dash" in {
      fts.replaceCharacters("abc\ndef") mustBe "abc-def"
    }

    "replace CR (\\r) with dash" in {
      fts.replaceCharacters("abc\rdef") mustBe "abc-def"
    }

    "replace FF (\\f) with dash" in {
      fts.replaceCharacters("abc\fdef") mustBe "abc-def"
    }

    "replace single quote with dash" in {
      fts.replaceCharacters("O'Brien") mustBe "O-Brien"
    }

    "replace tab (\\t) with dash" in {
      fts.replaceCharacters("abc\tdef") mustBe "abc-def"
    }

    "replace space with dash" in {
      fts.replaceCharacters("abc def") mustBe "abc-def"
    }

    "replace multiple special characters in sequence" in {
      fts.replaceCharacters("a\nb\rc") mustBe "a-b-c"
    }

    "leave alphanumeric content intact" in {
      fts.replaceCharacters("AR-B-LAB-001") mustBe "AR-B-LAB-001"
    }
  }

  // ─── toTsQueryInput ───────────────────────────────────────────────────────

  "toTsQueryInput" must {

    "return null for null input" in {
      fts.toTsQueryInput(null) mustBe null
    }

    "return empty string for empty input (no suffix)" in {
      fts.toTsQueryInput("") mustBe ""
    }

    "append :* suffix for non-empty input" in {
      fts.toTsQueryInput("AR") mustBe "AR:*"
    }

    "sanitize input before appending suffix" in {
      fts.toTsQueryInput("Juan Perez") mustBe "Juan-Perez:*"
    }

    "return dashes with suffix for whitespace-only input (no tsquery lexeme — DB returns 0 rows)" in {
      // spaces → dashes; "---".trim is non-empty → :* appended
      // tsquery '---:*' matches nothing in postgres simple dictionary
      fts.toTsQueryInput("   ") mustBe "---:*"
    }
  }

  // ─── statusFragment ───────────────────────────────────────────────────────

  "statusFragment" must {

    "return OR clause when both active and inactive are true" in {
      fts.statusFragment(active = true, inactive = true) mustBe
        """(pd."DELETED" = true OR pd."DELETED" = false)"""
    }

    "return only non-deleted filter when active=true inactive=false" in {
      fts.statusFragment(active = true, inactive = false) mustBe
        """pd."DELETED" = false"""
    }

    "return only deleted filter when active=false inactive=true" in {
      fts.statusFragment(active = false, inactive = true) mustBe
        """pd."DELETED" = true"""
    }

    "return '1=0' (match nothing) when both active and inactive are false" in {
      fts.statusFragment(active = false, inactive = false) mustBe "1=0"
    }
  }

  // ─── notUploadedFragment ─────────────────────────────────────────────────

  "notUploadedFragment" must {

    "return empty string when notUploaded is None" in {
      fts.notUploadedFragment(None) mustBe ""
    }

    "return empty string when notUploaded is Some(false)" in {
      fts.notUploadedFragment(Some(false)) mustBe ""
    }

    "return IS NULL clause when notUploaded is Some(true)" in {
      fts.notUploadedFragment(Some(true)) mustBe """AND pu."ID" IS NULL"""
    }
  }
