package unit.bulkupload

import bulkupload.GeneMapperFileMitoParser.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import profile.Mitocondrial

// #218 — funciones puras de validacion del genoma mitocondrial (sin legacy test).
// Genoma valido [1, 16569]; D-loop = posiciones <=576 o >=16024 (la zona (576,16024) es invalida).
class GeneMapperFileMitoParserTest extends AnyWordSpec with Matchers:

  "GeneMapperFileMitoParser.toInt" must {
    "parsear un entero valido" in { toInt("263") mustBe Some(263) }
    "devolver None para un valor no numerico" in { toInt("abc") mustBe None }
    "devolver None para un valor vacio" in { toInt("") mustBe None }
  }

  // validarMaxMin(max, min): max = RangeTo, min = RangeFrom.
  "GeneMapperFileMitoParser.validarMaxMin" must {
    "devolver E0308 cuando un extremo no es numerico" in {
      validarMaxMin("abc", "100") mustBe Some("E0308")
    }
    "devolver E0308 cuando un extremo esta vacio" in {
      validarMaxMin("", "100") mustBe Some("E0308")
    }
    "devolver E0308 cuando max supera el genoma (>16569)" in {
      validarMaxMin("17000", "100") mustBe Some("E0308")
    }
    "devolver E0308 cuando min es menor a 1" in {
      validarMaxMin("500", "0") mustBe Some("E0308")
    }
    "devolver E0312 cuando un extremo cae en la zona prohibida (576,16024)" in {
      validarMaxMin("1000", "100") mustBe Some("E0312")
    }
    "devolver E0309 cuando min >= max (ambos validos en el D-loop)" in {
      validarMaxMin("300", "500") mustBe Some("E0309")
    }
    "devolver None para un rango valido en el extremo bajo del D-loop" in {
      validarMaxMin("500", "100") mustBe None
    }
    "devolver None para un rango valido en el extremo alto del D-loop" in {
      validarMaxMin("16569", "16100") mustBe None
    }
  }

  "GeneMapperFileMitoParser.validarRangoVariaciones" must {
    "devolver E0307 cuando una variacion cae fuera del rango [min, max+0.9]" in {
      validarRangoVariaciones("300", "100", Seq("A500G")) mustBe Some("E0307")
    }
    "devolver E0311 cuando una variacion (dentro del rango) cae en la zona de referencia prohibida" in {
      validarRangoVariaciones("16100", "100", Seq("A1000G")) mustBe Some("E0311")
    }
    "devolver None cuando todas las variaciones son validas" in {
      validarRangoVariaciones("300", "100", Seq("A150G")) mustBe None
    }
    "devolver None para una lista de variaciones vacias" in {
      validarRangoVariaciones("300", "100", Seq("", "")) mustBe None
    }
  }

  "GeneMapperFileMitoParser.convertirPosiciones" must {
    "mapear una sustitucion a (Mitocondrial, base de referencia)" in {
      convertirPosiciones(List("A263G")) mustBe List((Mitocondrial('G', BigDecimal(263)), "A"))
    }
    "descartar (null, \"\") una insercion con posicion decimal" in {
      val res = convertirPosiciones(List("263.1C"))
      res.head._1 mustBe null
      res.head._2 mustBe ""
    }
    "descartar (null, \"\") un valor vacio" in {
      val res = convertirPosiciones(List(""))
      res.head._1 mustBe null
      res.head._2 mustBe ""
    }
  }
