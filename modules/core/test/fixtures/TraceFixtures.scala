package fixtures

import configdata.MatchingRule
import kits.AnalysisType
import matching.{Algorithm, Stringency}
import pedigree.{PedigreeDataCreation, PedigreeMetaData}
import profile.{AlleleValue, Profile}
import profile.GenotypificationByType.GenotypificationByType
import trace.*
import types.{AlphanumericId, SampleCode}

import java.util.Date

object TraceFixtures:

  val sampleCode  = SampleCode("AR-B-SHDG-1234")
  val sampleCode2 = SampleCode("AR-B-SHDG-1000")
  val traceDate   = new Date()

  val matchingRule = MatchingRule(
    `type`            = 1,
    categoryRelated   = AlphanumericId("SOSPECHOSO"),
    minimumStringency = Stringency.HighStringency,
    failOnMatch       = false,
    forwardToUpper    = false,
    matchingAlgorithm = Algorithm.ENFSI,
    minLocusMatch     = 10,
    mismatchsAllowed  = 0,
    considerForN      = false
  )

  val analysisType = AnalysisType(1, "Autosomal")

  val pedigreeMetaData   = PedigreeMetaData(15L, "Pedigree Test", "assignee1")
  val pedigreeData       = PedigreeDataCreation(pedigreeMetaData)

  val genotypification: GenotypificationByType = Map(1 -> Map("CSF1PO" -> List(AlleleValue("10"))))

  val sampleProfile = Profile(
    _id                    = sampleCode,
    globalCode             = sampleCode,
    internalSampleCode     = "ISC-001",
    assignee               = "assignee1",
    categoryId             = AlphanumericId("SOSPECHOSO"),
    genotypification       = genotypification,
    analyses               = None,
    labeledGenotypification = None,
    contributors           = Some(1),
    mismatches             = None,
    deleted                = false,
    matcheable             = true,
    isReference            = false,
    processed              = false
  )

  val hitInfo            = HitInfo("match-123", sampleCode2, "userId", 1)
  val hitTrace           = Trace(sampleCode, "userId", traceDate, hitInfo)
  val profileDataTrace   = Trace(sampleCode, "userId", traceDate, ProfileDataInfo)

  val pedigreeDiscardInfo = PedigreeDiscardInfo("match-456", 15L, "userId", 1)
  val pedigreeTrace       = TracePedigree(15L, "userId", traceDate, pedigreeDiscardInfo)

  val traceSearch         = TraceSearch(0, 30, sampleCode, "userId", isSuperUser = false)
  val traceSearchPedigree = TraceSearchPedigree(0, 30, 15, "userId", isSuperUser = false)
