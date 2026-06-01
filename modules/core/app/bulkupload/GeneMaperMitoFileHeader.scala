package bulkupload

case class GeneMaperMitoFileHeader(
  sampleName: Int, UD1: Int, SpecimenCategory: Int, RangeFrom: Int, RangeTo: Int,
  Variacion1: Int, Variacion2: Int, Variacion3: Int, Variacion4: Int, Variacion5: Int,
  Variacion6: Int, Variacion7: Int, Variacion8: Int, Variacion9: Int, Variacion10: Int,
  Variacion11: Int, Variacion12: Int, Variacion13: Int, Variacion14: Int, Variacion15: Int,
  Variacion16: Int, Variacion17: Int, Variacion18: Int, Variacion19: Int, Variacion20: Int,
  Variacion21: Int, Variacion22: Int, Variacion23: Int, Variacion24: Int, Variacion25: Int,
  Variacion26: Int, Variacion27: Int, Variacion28: Int, Variacion29: Int, Variacion30: Int,
  Variacion31: Int, Variacion32: Int, Variacion33: Int, Variacion34: Int, Variacion35: Int,
  Variacion36: Int, Variacion37: Int, Variacion38: Int, Variacion39: Int, Variacion40: Int,
  Variacion41: Int, Variacion42: Int, Variacion43: Int, Variacion44: Int, Variacion45: Int,
  Variacion46: Int, Variacion47: Int, Variacion48: Int, Variacion49: Int, Variacion50: Int,
  HeaderLine: Seq[String]
)

case class GeneMaperFileMitoHeaderBuilder(
  sampleName: Int = -1, UD1: Int = -1, SpecimenCategory: Int = -1,
  RangeFrom: Int = -1, RangeTo: Int = -1,
  Variacion1: Int = -1, Variacion2: Int = -1, Variacion3: Int = -1, Variacion4: Int = -1, Variacion5: Int = -1,
  Variacion6: Int = -1, Variacion7: Int = -1, Variacion8: Int = -1, Variacion9: Int = -1, Variacion10: Int = -1,
  Variacion11: Int = -1, Variacion12: Int = -1, Variacion13: Int = -1, Variacion14: Int = -1, Variacion15: Int = -1,
  Variacion16: Int = -1, Variacion17: Int = -1, Variacion18: Int = -1, Variacion19: Int = -1, Variacion20: Int = -1,
  Variacion21: Int = -1, Variacion22: Int = -1, Variacion23: Int = -1, Variacion24: Int = -1, Variacion25: Int = -1,
  Variacion26: Int = -1, Variacion27: Int = -1, Variacion28: Int = -1, Variacion29: Int = -1, Variacion30: Int = -1,
  Variacion31: Int = -1, Variacion32: Int = -1, Variacion33: Int = -1, Variacion34: Int = -1, Variacion35: Int = -1,
  Variacion36: Int = -1, Variacion37: Int = -1, Variacion38: Int = -1, Variacion39: Int = -1, Variacion40: Int = -1,
  Variacion41: Int = -1, Variacion42: Int = -1, Variacion43: Int = -1, Variacion44: Int = -1, Variacion45: Int = -1,
  Variacion46: Int = -1, Variacion47: Int = -1, Variacion48: Int = -1, Variacion49: Int = -1, Variacion50: Int = -1,
  HeaderLine: Seq[String] = Nil
):
  def buildWith(s: String, i: Int): GeneMaperFileMitoHeaderBuilder =
    s match
      case "Sample Name"       => copy(sampleName = i)
      case "UD1"               => copy(UD1 = i)
      case "Specimen Category" => copy(SpecimenCategory = i)
      case "Range From"        => copy(RangeFrom = i)
      case "Range To"          => copy(RangeTo = i)
      case "Mut1"              => copy(Variacion1 = i)
      case "Mut2"              => copy(Variacion2 = i)
      case "Mut3"              => copy(Variacion3 = i)
      case "Mut4"              => copy(Variacion4 = i)
      case "Mut5"              => copy(Variacion5 = i)
      case "Mut6"              => copy(Variacion6 = i)
      case "Mut7"              => copy(Variacion7 = i)
      case "Mut8"              => copy(Variacion8 = i)
      case "Mut9"              => copy(Variacion9 = i)
      case "Mut10"             => copy(Variacion10 = i)
      case "Mut11"             => copy(Variacion11 = i)
      case "Mut12"             => copy(Variacion12 = i)
      case "Mut13"             => copy(Variacion13 = i)
      case "Mut14"             => copy(Variacion14 = i)
      case "Mut15"             => copy(Variacion15 = i)
      case "Mut16"             => copy(Variacion16 = i)
      case "Mut17"             => copy(Variacion17 = i)
      case "Mut18"             => copy(Variacion18 = i)
      case "Mut19"             => copy(Variacion19 = i)
      case "Mut20"             => copy(Variacion20 = i)
      case "Mut21"             => copy(Variacion21 = i)
      case "Mut22"             => copy(Variacion22 = i)
      case "Mut23"             => copy(Variacion23 = i)
      case "Mut24"             => copy(Variacion24 = i)
      case "Mut25"             => copy(Variacion25 = i)
      case "Mut26"             => copy(Variacion26 = i)
      case "Mut27"             => copy(Variacion27 = i)
      case "Mut28"             => copy(Variacion28 = i)
      case "Mut29"             => copy(Variacion29 = i)
      case "Mut30"             => copy(Variacion30 = i)
      case "Mut31"             => copy(Variacion31 = i)
      case "Mut32"             => copy(Variacion32 = i)
      case "Mut33"             => copy(Variacion33 = i)
      case "Mut34"             => copy(Variacion34 = i)
      case "Mut35"             => copy(Variacion35 = i)
      case "Mut36"             => copy(Variacion36 = i)
      case "Mut37"             => copy(Variacion37 = i)
      case "Mut38"             => copy(Variacion38 = i)
      case "Mut39"             => copy(Variacion39 = i)
      case "Mut40"             => copy(Variacion40 = i)
      case "Mut41"             => copy(Variacion41 = i)
      case "Mut42"             => copy(Variacion42 = i)
      case "Mut43"             => copy(Variacion43 = i)
      case "Mut44"             => copy(Variacion44 = i)
      case "Mut45"             => copy(Variacion45 = i)
      case "Mut46"             => copy(Variacion46 = i)
      case "Mut47"             => copy(Variacion47 = i)
      case "Mut48"             => copy(Variacion48 = i)
      case "Mut49"             => copy(Variacion49 = i)
      case "Mut50"             => copy(Variacion50 = i)
      case _                   => this

  def build: Either[String, GeneMaperMitoFileHeader] =
    if sampleName > -1 && UD1 > -1 && SpecimenCategory > -1 && RangeFrom > -1 && RangeTo > -1 &&
      Variacion1 > -1 && Variacion2 > -1 && Variacion3 > -1 && Variacion4 > -1 && Variacion5 > -1 &&
      Variacion6 > -1 && Variacion7 > -1 && Variacion8 > -1 && Variacion9 > -1 && Variacion10 > -1 &&
      Variacion11 > -1 && Variacion12 > -1 && Variacion13 > -1 && Variacion14 > -1 && Variacion15 > -1 &&
      Variacion16 > -1 && Variacion17 > -1 && Variacion18 > -1 && Variacion19 > -1 && Variacion20 > -1 &&
      Variacion21 > -1 && Variacion22 > -1 && Variacion23 > -1 && Variacion24 > -1 && Variacion25 > -1 &&
      Variacion26 > -1 && Variacion27 > -1 && Variacion28 > -1 && Variacion29 > -1 && Variacion30 > -1 &&
      Variacion31 > -1 && Variacion32 > -1 && Variacion33 > -1 && Variacion34 > -1 && Variacion35 > -1 &&
      Variacion36 > -1 && Variacion37 > -1 && Variacion38 > -1 && Variacion39 > -1 && Variacion40 > -1 &&
      Variacion41 > -1 && Variacion42 > -1 && Variacion43 > -1 && Variacion44 > -1 && Variacion45 > -1 &&
      Variacion46 > -1 && Variacion47 > -1 && Variacion48 > -1 && Variacion49 > -1 && Variacion50 > -1
    then Right(GeneMaperMitoFileHeader(
      sampleName, UD1, SpecimenCategory, RangeFrom, RangeTo,
      Variacion1, Variacion2, Variacion3, Variacion4, Variacion5,
      Variacion6, Variacion7, Variacion8, Variacion9, Variacion10,
      Variacion11, Variacion12, Variacion13, Variacion14, Variacion15,
      Variacion16, Variacion17, Variacion18, Variacion19, Variacion20,
      Variacion21, Variacion22, Variacion23, Variacion24, Variacion25,
      Variacion26, Variacion27, Variacion28, Variacion29, Variacion30,
      Variacion31, Variacion32, Variacion33, Variacion34, Variacion35,
      Variacion36, Variacion37, Variacion38, Variacion39, Variacion40,
      Variacion41, Variacion42, Variacion43, Variacion44, Variacion45,
      Variacion46, Variacion47, Variacion48, Variacion49, Variacion50,
      HeaderLine))
    else Left("error.E0305: formato de archivo mitocondrial inválido, faltan columnas obligatorias")