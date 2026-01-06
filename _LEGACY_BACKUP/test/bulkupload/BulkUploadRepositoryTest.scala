package bulkupload

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import specs.PdgSpec
import types.SampleCode

class BulkUploadRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)
  
  "A BulkUploadRepository" must {
    "get batches for step 1" in {
      
      val repo = new SlickProtoProfileRepository(null,"",app)
      
      val result = Await.result(repo.getBatchesStep1("user", false), duration)
      
      result.size mustBe 0
    }
    
    "tell if a sample already exists" in {
      
      val repo = new SlickProtoProfileRepository(null,"",app)
      
      val result = Await.result(repo.exists("SMG-1"), duration)
      
      result._1 mustBe (Some(SampleCode("AR-C-SHDG-1")))
      result._2 mustBe None
    }
    
  }
  
}