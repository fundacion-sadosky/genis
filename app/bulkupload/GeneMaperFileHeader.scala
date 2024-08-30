package bulkupload

import play.api.i18n.{Messages, MessagesApi}
import play.i18n.MessagesApi

case class GeneMaperFileHeader(
  sampleName: Int,
  UD1: Int,
  Marker: Int,
  UD2: Int,
  SpecimenCategory: Int,
  Allele1: Int,
  Allele2: Int,
  Allele3: Int,
  Allele4: Int,
  Allele5: Int,
  Allele6: Int,
  Allele7: Int,
  Allele8: Int,
  HeaderLine: Seq[String])

case class GeneMaperFileHeaderBuilder(
  sampleName: Int = -1, 
  UD1: Int = -1, 
  Marker: Int = -1, 
  UD2: Int = -1, 
  SpecimenCategory: Int = -1, 
  Allele1: Int = -1, 
  Allele2: Int = -1, 
  Allele3: Int = -1, 
  Allele4: Int = -1, 
  Allele5: Int = -1, 
  Allele6: Int = -1, 
  Allele7: Int = -1, 
  Allele8: Int = -1,
  HeaderLine: Seq[String] = Nil) {

  def buildWith(s: String, i: Int): GeneMaperFileHeaderBuilder = {
    s match {
      case "Sample Name"       => GeneMaperFileHeaderBuilder(i, this.UD1, this.Marker, this.UD2, this.SpecimenCategory, this.Allele1, this.Allele2, this.Allele3, this.Allele4, this.Allele5, this.Allele6, this.Allele7, this.Allele8, this.HeaderLine)
      case "UD1"               => GeneMaperFileHeaderBuilder(this.sampleName, i, this.Marker, this.UD2, this.SpecimenCategory, this.Allele1, this.Allele2, this.Allele3, this.Allele4, this.Allele5, this.Allele6, this.Allele7, this.Allele8, this.HeaderLine)
      case "Marker"            => GeneMaperFileHeaderBuilder(this.sampleName, this.UD1, i, this.UD2, this.SpecimenCategory, this.Allele1, this.Allele2, this.Allele3, this.Allele4, this.Allele5, this.Allele6, this.Allele7, this.Allele8, this.HeaderLine)
      case "UD2"               => GeneMaperFileHeaderBuilder(this.sampleName, this.UD1, this.Marker, i, this.SpecimenCategory, this.Allele1, this.Allele2, this.Allele3, this.Allele4, this.Allele5, this.Allele6, this.Allele7, this.Allele8, this.HeaderLine)
      case "Specimen Category" => GeneMaperFileHeaderBuilder(this.sampleName, this.UD1, this.Marker, this.UD2, i, this.Allele1, this.Allele2, this.Allele3, this.Allele4, this.Allele5, this.Allele6, this.Allele7, this.Allele8, this.HeaderLine)
      case "Allele 1"          => GeneMaperFileHeaderBuilder(this.sampleName, this.UD1, this.Marker, this.UD2, this.SpecimenCategory, i, this.Allele2, this.Allele3, this.Allele4, this.Allele5, this.Allele6, this.Allele7, this.Allele8, this.HeaderLine)
      case "Allele 2"          => GeneMaperFileHeaderBuilder(this.sampleName, this.UD1, this.Marker, this.UD2, this.SpecimenCategory, this.Allele1, i, this.Allele3, this.Allele4, this.Allele5, this.Allele6, this.Allele7, this.Allele8, this.HeaderLine)
      case "Allele 3"          => GeneMaperFileHeaderBuilder(this.sampleName, this.UD1, this.Marker, this.UD2, this.SpecimenCategory, this.Allele1, this.Allele2, i, this.Allele4, this.Allele5, this.Allele6, this.Allele7, this.Allele8, this.HeaderLine)
      case "Allele 4"          => GeneMaperFileHeaderBuilder(this.sampleName, this.UD1, this.Marker, this.UD2, this.SpecimenCategory, this.Allele1, this.Allele2, this.Allele3, i, this.Allele5, this.Allele6, this.Allele7, this.Allele8, this.HeaderLine)
      case "Allele 5"          => GeneMaperFileHeaderBuilder(this.sampleName, this.UD1, this.Marker, this.UD2, this.SpecimenCategory, this.Allele1, this.Allele2, this.Allele3, this.Allele4, i, this.Allele6, this.Allele7, this.Allele8, this.HeaderLine)
      case "Allele 6"          => GeneMaperFileHeaderBuilder(this.sampleName, this.UD1, this.Marker, this.UD2, this.SpecimenCategory, this.Allele1, this.Allele2, this.Allele3, this.Allele4, this.Allele5, i, this.Allele7, this.Allele8, this.HeaderLine)
      case "Allele 7"          => GeneMaperFileHeaderBuilder(this.sampleName, this.UD1, this.Marker, this.UD2, this.SpecimenCategory, this.Allele1, this.Allele2, this.Allele3, this.Allele4, this.Allele5, this.Allele6, i, this.Allele8, this.HeaderLine)
      case "Allele 8"          => GeneMaperFileHeaderBuilder(this.sampleName, this.UD1, this.Marker, this.UD2, this.SpecimenCategory, this.Allele1, this.Allele2, this.Allele3, this.Allele4, this.Allele5, this.Allele6, this.Allele7, i, this.HeaderLine) 
      case _                   => this
    }
  }

  def build (implicit messages: Messages): Either[String, GeneMaperFileHeader] = {
    if (this.sampleName > -1 && this.UD1 > -1 && this.Marker > -1 &&
      this.SpecimenCategory > -1 && this.Allele1 > -1 && this.Allele2 > -1 &&
      this.Allele3 > -1 && this.Allele4 > -1 && this.Allele5 > -1 && this.Allele6 > -1 &&
      this.Allele7 > -1 && this.Allele8 > -1)
      Right(GeneMaperFileHeader(this.sampleName, this.UD1, this.Marker, this.UD2, this.SpecimenCategory, this.Allele1, this.Allele2, this.Allele3, this.Allele4, this.Allele5, this.Allele6, this.Allele7, this.Allele8, this.HeaderLine))
    else
      Left(messages("error.E0305"))
  }
}