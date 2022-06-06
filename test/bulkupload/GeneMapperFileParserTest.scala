package bulkupload

import java.io.File

import scala.language.implicitConversions
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar
import profile.Allele
import specs.PdgSpec
import types.AlphanumericId
import types.SampleCode


class GeneMapperFileParserTest extends PdgSpec with MockitoSugar with AnswerSugar {

  "GeneMapperFileParser" must {
    "not swallow first line when switch to a new profile" in {

      val csvFile = new File("test/resources/GenemapperJunin.csv")

      val validator = mock[Validator]

      when(validator.validateSampleName(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        (Some(invocation.getArguments()(0)),None)
      })
      when(validator.validateAssignee(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        Some(invocation.getArguments()(0))
      })
      when(validator.validateCategory(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        None
      })
      when(validator.validateAssigneAndCategory(any[SampleCode], any[String], any[Option[AlphanumericId]]))
        .thenAnswer((invocation: InvocationOnMock) => { None })
      when(validator.validateKit(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        (Some(invocation.getArguments()(0)), invocation.getArguments()(0))
      })
      when(validator.validateMarker(any[String], any[String], any[Boolean])).thenAnswer((invocation: InvocationOnMock) => {
        (Seq(invocation.getArguments()(1)), invocation.getArguments()(1))
      })

      GeneMapperFileParser.parse(csvFile, validator).fold({ error =>
        fail(error)
      }, { stream =>
        val profiles = stream.take(3).toList

        val p1 = profiles(0)
        val p2 = profiles(1)
        val p3 = profiles(2)

        println(p1.genotypification.size)
        println(p2.genotypification.size)
        println(p3.genotypification.size)

        p1.genotypification.exists(_.locus == "AMEL") must be(true)
        p2.genotypification.exists(_.locus == "AMEL") must be(true)
        p3.genotypification.exists(_.locus == "AMEL") must be(true)

      })
    }
    "accept homocygotes in both ways" in {

      val csvFile = new File("test/resources/Homocygote.csv")

      val validator = mock[Validator]

      when(validator.validateSampleName(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        (Some(invocation.getArguments()(0)),None)
      })
      when(validator.validateAssignee(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        Some(invocation.getArguments()(0))
      })
      when(validator.validateCategory(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        None
      })
      when(validator.validateAssigneAndCategory(any[SampleCode], any[String], any[Option[AlphanumericId]]))
        .thenAnswer((invocation: InvocationOnMock) => { None })
      when(validator.validateKit(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        (Some(invocation.getArguments()(0)), invocation.getArguments()(0))
      })
      when(validator.validateMarker(any[String], any[String], any[Boolean])).thenAnswer((invocation: InvocationOnMock) => {
        (Seq(invocation.getArguments()(1)), invocation.getArguments()(1))
      })

      GeneMapperFileParser.parse(csvFile, validator).fold({ error =>
        fail(error)
      }, { stream =>
        val profiles = stream.toList

        val p1 = profiles(0)
        val p2 = profiles(1)

        p1.genotypification.find(_.locus == "FGA").get.alleles must be(List(Allele(16)))
        p2.genotypification.find(_.locus == "FGA").get.alleles must be(List(Allele(16), Allele(16)))

      })
    }

    "accept alleles with , or ." in {

      val csvFile = new File("test/resources/AllelesFormat.csv")

      val validator = mock[Validator]

      when(validator.validateSampleName(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        (Some(invocation.getArguments()(0)),None)
      })
      when(validator.validateAssignee(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        Some(invocation.getArguments()(0))
      })
      when(validator.validateCategory(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        None
      })
      when(validator.validateAssigneAndCategory(any[SampleCode], any[String], any[Option[AlphanumericId]]))
        .thenAnswer((invocation: InvocationOnMock) => { None })
      when(validator.validateKit(any[String])).thenAnswer((invocation: InvocationOnMock) => {
        (Some(invocation.getArguments()(0)), invocation.getArguments()(0))
      })
      when(validator.validateMarker(any[String], any[String],any[Boolean])).thenAnswer((invocation: InvocationOnMock) => {
        (Seq(invocation.getArguments()(1)), invocation.getArguments()(1))
      })

      GeneMapperFileParser.parse(csvFile, validator).fold({ error =>
        fail(error)
      }, { stream =>
        val profiles = stream.toList

        val p1 = profiles(0)
        val p2 = profiles(1)

        p1.genotypification.find(_.locus == "FGA").get.alleles must be(List(Allele(13.2)))
        p2.genotypification.find(_.locus == "FGA").get.alleles must be(List(Allele(13.2)))

      })
    }

  }

}