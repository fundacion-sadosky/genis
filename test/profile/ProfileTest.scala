package profile

import org.specs2.mutable.Specification
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.bson.BSONObjectID
import stubs.Stubs
import types.SampleCode
import types.AlphanumericId
import play.api.libs.json.JsPath

class ProfileTest extends Specification {

  "AlleleValue" should {
    "parse from json" in {
      val jsonString = """{"TPOX" : [22.2]}"""

      val json = Json.parse(jsonString)
      val genotype = Json.fromJson[Map[Profile.Marker, List[AlleleValue]]](json).asOpt

      genotype must beSome
      genotype.get must haveKey("TPOX")
      genotype.get("TPOX") must have size (1)
      genotype.get("TPOX") must contain(Allele(22.2))
    }
  }

  "AlleleValue" should {
    "parses Allele" in {
      Json.fromJson[AlleleValue](Json.parse("22.2")).asOpt.get must beEqualTo(Allele(22.2))
    }
    "be contructed for mitochondrial homopoymeric regions from string with '-' as first character" in {
      val text: String = "-16500.1A"
      val allele = AlleleValue(text)
      allele match {
        case Mitocondrial(base, pos) =>
          (base must beEqualTo('A')) and
            (pos must beEqualTo(16500.1)).toResult
        case _ => failure("show")
      }
    }
    "should fail to contruct  mitochondrial homopolymeric region from string not starting with '-'" in {
      val text: String = "16500.1A"
      AlleleValue(text) must throwA[IllegalArgumentException]
    }
  }

  "AlleleValue" should {
    "parses Allele from string" in {
      Json.fromJson[AlleleValue](Json.parse("\"23.7\"")).asOpt.get must beEqualTo(Allele(23.7))

    }
  }

//  "AlleleValue" should {
//    "parses sequenced" in {
//      Json.fromJson[AlleleValue](Json.parse(""""22.2:33"""")).asOpt.get must beEqualTo(SequencedAllele(22.2, 33))
//
//    }
//  }

  "oid" should {
    "be parseable" in {
      val jsonString = """{"$oid":"54218f877a4c6f09ad289b23"}"""

      val json = Json.parse(jsonString)

      val maybeProfile = Json.fromJson[BSONObjectID](json).asOpt

      maybeProfile must beSome
      maybeProfile.get must beEqualTo(BSONObjectID("54218f877a4c6f09ad289b23"))
    }
  }

  "Profile" should {
    "parse from json" in {
      val jsonString = """{
		    "_id" : "AR-B-LAB-1",
    		"categoryId" : "someCat",
    		"subcategoryId" : "someSubcat",
		    "globalCode" : "AR-B-LAB-1",
        "internalSampleCode" : "internal",
        "assignee" : "tst-admintist",
        "deleted" : false,
        "matcheable" : true,
        "isReference" : true,
		    "genotypification" : { "1": {
		        "TPOX" : [28.2, "29"],
		        "D3S1358" : ["28", "29"]
          }
		    },
	    	"analyses" : [{
    		  	"id" : "an1",
    		  	"date" : { "$date": "2014-01-01" },
	    		"kit" : "someStrKit",
	    		"genotypification" : {
    		  		"TPOX" : [28.2, "29"],
			        "D3S1358" : ["28", "23"],
    		  		"Mt" : ["A@1", "-@2", "C@1.1"]
    		  	}
    		  },{
    		  	"id" : "an2",
    		  	"date" : { "$date": "2014-01-02" },
    		  	"kit" : "otherStrKit",
    		  	"genotypification" : {
		        	"DXS10146" : [23, 35, "X"],
		        	"DXS10148" : [31, "Y"]
    		  	}
    		  }]
        }"""

      val json = Json.parse(jsonString)

      val maybeProfile = Json.fromJson[Profile](json).asOpt

      maybeProfile must beSome
      maybeProfile.get._id must beEqualTo(SampleCode("AR-B-LAB-1"))
      maybeProfile.get.categoryId must beEqualTo(AlphanumericId("someCat"))
      maybeProfile.get.assignee must beEqualTo("tst-admintist")
      maybeProfile.get.globalCode must beEqualTo(SampleCode("AR-B-LAB-1"))
      maybeProfile.get.internalSampleCode must beEqualTo("internal")
      maybeProfile.get.genotypification(1) must have size (2)
      maybeProfile.get.genotypification(1) must haveKeys("TPOX", "D3S1358")
      maybeProfile.get.genotypification(1).get("TPOX").get must contain(Allele(28.2))

      val analyses: List[Analysis] = maybeProfile.get.analyses.get
      analyses.size must beEqualTo(2)
      analyses(0).kit must beEqualTo("someStrKit")
      //maybeProfile.get.analyses(0).date must beEqualTo(new Date("2014-01-01"))
      analyses(0).genotypification must have size (3)
      analyses(1).kit must beEqualTo("otherStrKit")
      //maybeProfile.get.analyses(1).date must beEqualTo(new Date("2014-01-02"))
      analyses(1).genotypification must have size (2)
      analyses(1).genotypification must haveKeys("DXS10146", "DXS10148")
//      analyses(1).genotypification.get("DXS10146").get must have size (3)
//      analyses(1).genotypification.get("DXS10148").get must have size (2)
      analyses(1).genotypification.get("DXS10146").get must contain(XY('X'))
      analyses(1).genotypification.get("DXS10148").get must contain(XY('Y'))
      analyses(0).genotypification.get("Mt").get must contain(Mitocondrial('A', 1))
      analyses(0).genotypification.get("Mt").get must contain(Mitocondrial('-', 2))
      analyses(0).genotypification.get("Mt").get must contain(Mitocondrial('C', 1.1))
    }
  }

  "Profile" should {
    "write to json" in {

      val profile = Stubs.newProfile

      val maybeJsonProfile = Json.toJson(profile)

      maybeJsonProfile must not beNull

      val id = (maybeJsonProfile \ "_id").as[SampleCode]

      id must beEqualTo(profile._id)

    }
  }

  "Profile" should {
    "fail if an invalid Mitocondrial allele json is provided" in {
      val jsonString = """{
		    "_id" : "AR-B-LAB-1",
    		"categoryId" : "someCat",
    		"subcategoryId" : "someSubcat",
		    "globalCode" : "AR-B-LAB-1",
        "matcheable" : true,
        "isReference" : true,
		    "genotypification" : { "1": {
		        "TPOX" : [28.2, "29"],
		        "D3S1358" : ["28", "29"]
          }
		    },
	    	"analyses" : [{
    		  	"id" : "an1",
    		  	"date" : { "$date": "2014-01-01" },
	    		"kit" : "someStrKit",
	    		"genotypification" : {
    		  		"Mt" : ["YY@1"]
    		  	}
    		  }]
        }"""

      val json = Json.parse(jsonString)
      val jsonResult = Json.fromJson[Profile](json)
      jsonResult.fold({ invalid =>
        invalid(0)._1 must beEqualTo(((JsPath \ "analyses")(0) \ "genotypification" \ "Mt")(0))
        invalid(0)._2(0).message must contain("Allele")
      }, valid => failure(jsonString + " wasn't able to be parsed"))

      jsonResult.isError must beTrue
    }

    "fail if an invalid Mitocondrial allele json is provided" in {
      val jsonString = """{
		    "_id" : "AR-B-LAB-1",
    		"categoryId" : "someCat",
    		"subcategoryId" : "someSubcat",
		    "globalCode" : "AR-B-LAB-1",
        "matcheable" : true,
        "isReference" : true,
		    "genotypification" : { "1": {
		        "TPOX" : [28.2, "29"],
		        "D3S1358" : ["28", "29"]
          }
		    },
	    	"analyses" : [{
    		  	"id" : "an1",
    		  	"date" : { "$date": "2014-01-01" },
	    		"kit" : "someStrKit",
	    		"genotypification" : {
    		  		"Mt" : ["A@1.7"]
    		  	}
    		  }]
        }"""

      val json = Json.parse(jsonString)
      val jsonResult = Json.fromJson[Profile](json)
      jsonResult.fold({ invalid =>
        invalid(0)._1 must beEqualTo(((JsPath \ "analyses")(0) \ "genotypification" \ "Mt")(0))
        invalid(0)._2(0).message must contain("Allele")
      }, valid => failure(jsonString + " wasn't able to be parsed"))

      jsonResult.isError must beTrue
    }

  }

}