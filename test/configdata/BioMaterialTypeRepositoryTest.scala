package configdata

import specs.PdgSpec
import types.AlphanumericId

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class BioMaterialTypeRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)

  "A BioMaterialTypeRepository" must {
    "update material type description over 100 characters" in {
      val materialRepository = new SlickBioMaterialTypeRepository
      val bmt = BioMaterialType(AlphanumericId("SANGRE"), "Sangre",
        Some("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"))
      val result = Await.result(materialRepository.update(bmt), duration)

      result mustBe 1
    }

    "update material type description under 100 characters" in {
      val materialRepository = new SlickBioMaterialTypeRepository
      val bmt = BioMaterialType(AlphanumericId("SANGRE"), "SANGRE", Some("Sangre"))
      val result = Await.result(materialRepository.update(bmt), duration)

      result mustBe 1
    }

    "update material type with no description" in {
      val materialRepository = new SlickBioMaterialTypeRepository
      val bmt = BioMaterialType(AlphanumericId("SANGRE"), "SANGRE", None)
      val result = Await.result(materialRepository.update(bmt), duration)

      result mustBe 1
    }

  }

}