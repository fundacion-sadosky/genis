package standalone

import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

/** The compiled `object MutationService` (standalone/) must be identical, line by line, to
  * the one in the original GENis file (app/pedigree/MutationService.scala), and the extract
  * file must contain nothing else: only comments and the package clause before the marker. */
class MutationServiceExtractTest extends FlatSpec with Matchers {

  private def lines(path: String): List[String] = {
    val src = Source.fromFile(path, "UTF-8")
    try src.getLines().toList finally src.close()
  }

  "The MutationService extract" should "match the object in the original file" in {
    val original = lines("app/pedigree/MutationService.scala")
      .dropWhile(_ != "object MutationService {")
    val (header, rest) = lines("standalone/pedigree/MutationServiceObject.scala")
      .span(_ != "// ---- extract begins ----")

    original should not be empty
    header.filterNot(l => l.startsWith("//") || l.trim.isEmpty) shouldBe List("package pedigree")
    rest.drop(1) shouldBe original
  }
}
