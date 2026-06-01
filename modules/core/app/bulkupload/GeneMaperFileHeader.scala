package bulkupload

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
  HeaderLine: Seq[String]
)

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
  HeaderLine: Seq[String] = Nil
):
  def buildWith(s: String, i: Int): GeneMaperFileHeaderBuilder =
    s match
      case "Sample Name"       => copy(sampleName = i)
      case "UD1"               => copy(UD1 = i)
      case "Marker"            => copy(Marker = i)
      case "UD2"               => copy(UD2 = i)
      case "Specimen Category" => copy(SpecimenCategory = i)
      case "Allele 1"          => copy(Allele1 = i)
      case "Allele 2"          => copy(Allele2 = i)
      case "Allele 3"          => copy(Allele3 = i)
      case "Allele 4"          => copy(Allele4 = i)
      case "Allele 5"          => copy(Allele5 = i)
      case "Allele 6"          => copy(Allele6 = i)
      case "Allele 7"          => copy(Allele7 = i)
      case "Allele 8"          => copy(Allele8 = i)
      case _                   => this

  def build: Either[String, GeneMaperFileHeader] =
    if sampleName > -1 && UD1 > -1 && Marker > -1 &&
      SpecimenCategory > -1 && Allele1 > -1 && Allele2 > -1 &&
      Allele3 > -1 && Allele4 > -1 && Allele5 > -1 && Allele6 > -1 &&
      Allele7 > -1 && Allele8 > -1
    then Right(GeneMaperFileHeader(sampleName, UD1, Marker, UD2, SpecimenCategory,
      Allele1, Allele2, Allele3, Allele4, Allele5, Allele6, Allele7, Allele8, HeaderLine))
    else Left("error.E0305: formato de archivo GeneMapper inválido, faltan columnas obligatorias")